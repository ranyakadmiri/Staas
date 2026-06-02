#!/usr/bin/env python3
"""
STaaS Agent
-----------
Registers this machine with the STaaS platform and polls for
attach / detach commands, executing them via the local iSCSI stack.

Supported OS:
  Linux   — open-iscsi (iscsiadm)
  Windows — Microsoft iSCSI Initiator (iscsicli)
  macOS   — IQN detection only; attach/detach not natively supported

Usage:
  python staas_agent.py register --server http://localhost:8080 --token <user-jwt>
  python staas_agent.py start
  python staas_agent.py status
"""

import argparse
import json
import logging
import os
import platform
import subprocess
import sys
import time
from pathlib import Path

import requests

# ── Config paths ──────────────────────────────────────────────────────────────

CONFIG_DIR  = Path.home() / ".staas"
CONFIG_FILE = CONFIG_DIR / "config.json"
LOG_FILE    = CONFIG_DIR / "agent.log"

POLL_INTERVAL_SECONDS = 10

# ── Logging ───────────────────────────────────────────────────────────────────

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
log = logging.getLogger("staas-agent")


def enable_file_logging():
    CONFIG_DIR.mkdir(parents=True, exist_ok=True)
    fh = logging.FileHandler(LOG_FILE)
    fh.setFormatter(logging.Formatter("%(asctime)s [%(levelname)s] %(message)s"))
    log.addHandler(fh)


# ── Config helpers ────────────────────────────────────────────────────────────

def load_config() -> dict:
    if not CONFIG_FILE.exists():
        log.error("Not registered. Run: python staas_agent.py register --server <url> --token <user-jwt>")
        sys.exit(1)
    with open(CONFIG_FILE) as f:
        return json.load(f)


def save_config(data: dict):
    CONFIG_DIR.mkdir(parents=True, exist_ok=True)
    with open(CONFIG_FILE, "w") as f:
        json.dump(data, f, indent=2)
    log.info("Config saved → %s", CONFIG_FILE)


# ── OS helpers ────────────────────────────────────────────────────────────────

def detect_os() -> str:
    s = platform.system().lower()
    if s == "linux":   return "linux"
    if s == "windows": return "windows"
    if s == "darwin":  return "macos"
    return "unknown"


def get_initiator_iqn() -> str:
    os_name = detect_os()

    if os_name == "linux":
        path = Path("/etc/iscsi/initiatorname.iscsi")
        if not path.exists():
            raise RuntimeError("open-iscsi not installed. Run: sudo apt install open-iscsi")
        for line in path.read_text().splitlines():
            if line.startswith("InitiatorName="):
                return line.split("=", 1)[1].strip()
        raise RuntimeError("Could not parse IQN from /etc/iscsi/initiatorname.iscsi")

    if os_name == "windows":
        result = _run(["iscsicli", "reportinitiatoriqns"], check=False)
        for line in result.stdout.splitlines():
            line = line.strip()
            if line.startswith("iqn."):
                return line
        raise RuntimeError(
            "Could not detect IQN. Ensure the Microsoft iSCSI Initiator service is running.")

    if os_name == "macos":
        raise RuntimeError(
            "macOS has no native iSCSI initiator. "
            "Install globalSAN or use a Linux/Windows VM.")

    raise RuntimeError(f"Unsupported OS: {os_name}")


# ── iSCSI operations ──────────────────────────────────────────────────────────

def attach_volume(target_iqn: str, portal: str):
    os_name = detect_os()
    log.info("Attaching %s via %s", target_iqn, portal)

    if os_name == "linux":
        _run(["sudo", "iscsiadm", "-m", "discovery", "-t", "sendtargets", "-p", portal])
        _run(["sudo", "iscsiadm", "-m", "node", "-T", target_iqn, "-p", portal, "--login"])
        log.info("Attached. Run 'lsblk' to see the new device.")

    elif os_name == "windows":
        host, port = portal.split(":")
        _run(["iscsicli", "QAddTargetPortal", host, port])
        _run(["iscsicli", "QLoginTarget", target_iqn])
        log.info("Attached. Check Disk Management for the new disk.")

    else:
        raise RuntimeError(f"iSCSI attach not supported on {os_name}.")


def detach_volume(target_iqn: str, portal: str):
    os_name = detect_os()
    log.info("Detaching %s", target_iqn)

    if os_name == "linux":
        _run(["sudo", "iscsiadm", "-m", "node", "-T", target_iqn, "-p", portal, "--logout"])
        _run(["sudo", "iscsiadm", "-m", "node", "-T", target_iqn, "-p", portal, "--op", "delete"])

    elif os_name == "windows":
        _run(["iscsicli", "LogoutTarget", target_iqn])

    else:
        raise RuntimeError(f"iSCSI detach not supported on {os_name}.")


# ── API client ────────────────────────────────────────────────────────────────

class STaaSClient:

    def __init__(self, server: str, agent_jwt: str):
        self.server  = server.rstrip("/")
        self.headers = {
            "Authorization": f"Bearer {agent_jwt}",
            "Content-Type":  "application/json",
        }

    def get_pending_commands(self) -> list:
        r = requests.get(f"{self.server}/api/agents/me/commands",
                         headers=self.headers, timeout=10)
        r.raise_for_status()
        return r.json()

    def ack_command(self, command_id: int, success: bool, message: str):
        requests.post(
            f"{self.server}/api/agents/me/commands/{command_id}/ack",
            headers=self.headers,
            json={"success": success, "message": message},
            timeout=10,
        )

    def heartbeat(self):
        requests.post(f"{self.server}/api/agents/me/heartbeat",
                      headers=self.headers, timeout=5)


# ── CLI commands ──────────────────────────────────────────────────────────────

def cmd_register(args):
    """
    Register this machine.
    --token is the USER's login JWT from the STaaS dashboard.
    On success the agent JWT is saved to ~/.staas/config.json.
    """
    log.info("Detecting initiator IQN...")
    try:
        iqn = get_initiator_iqn()
    except RuntimeError as e:
        log.error("%s", e)
        sys.exit(1)

    log.info("IQN detected: %s", iqn)
    log.info("Registering with %s ...", args.server)

    try:
        r = requests.post(
            f"{args.server.rstrip('/')}/api/agents/register",
            headers={
                "Authorization": f"Bearer {args.token}",
                "Content-Type":  "application/json",
            },
            json={
                "hostname":     platform.node(),
                "initiatorIqn": iqn,
                "os":           detect_os(),
            },
            timeout=15,
        )
        r.raise_for_status()
    except requests.HTTPError:
        log.error("Registration failed (%s): %s", r.status_code, r.text)
        sys.exit(1)
    except requests.ConnectionError:
        log.error("Cannot reach %s — is the server running?", args.server)
        sys.exit(1)

    data = r.json()
    jwt  = data.get("jwt")

    if not jwt:
        log.error("Server did not return an agent JWT: %s", data)
        sys.exit(1)

    save_config({
        "server":       args.server.rstrip("/"),
        "agentJwt":     jwt,
        "agentId":      data.get("agentId"),
        "hostname":     platform.node(),
        "initiatorIqn": iqn,
        "os":           detect_os(),
    })

    log.info("Registered successfully. Agent ID: %s", data.get("agentId"))
    log.info("Run 'python staas_agent.py start' to begin polling.")


def cmd_start(args):
    """Poll for commands indefinitely."""
    enable_file_logging()
    config = load_config()
    client = STaaSClient(config["server"], config["agentJwt"])

    log.info("Agent started (id=%s). Polling every %ds. Ctrl+C to stop.",
             config.get("agentId"), POLL_INTERVAL_SECONDS)

    while True:
        try:
            client.heartbeat()
            commands = client.get_pending_commands()

            for command in commands:
                _handle_command(client, command)

        except requests.ConnectionError:
            log.warning("Server unreachable. Retrying in %ds.", POLL_INTERVAL_SECONDS)
        except requests.HTTPError as e:
            if e.response.status_code == 401:
                log.error("Agent JWT rejected. Re-register the agent.")
                sys.exit(1)
            log.warning("Server error: %s", e)
        except Exception as e:
            log.error("Unexpected error: %s", e)

        time.sleep(POLL_INTERVAL_SECONDS)


def cmd_status(args):
    """Print agent registration info (no secrets)."""
    if not CONFIG_FILE.exists():
        print("Not registered.")
        sys.exit(0)
    config = load_config()
    print(json.dumps(
        {k: v for k, v in config.items() if k != "agentJwt"},
        indent=2
    ))


# ── Command handler ───────────────────────────────────────────────────────────

def _handle_command(client: STaaSClient, command: dict):
    cid    = command["id"]
    ctype  = command["type"]          # ATTACH | DETACH
    iqn    = command["targetIqn"]
    portal = command["portalAddress"] # host:port

    log.info("Command #%d: %s %s via %s", cid, ctype, iqn, portal)

    try:
        if ctype == "ATTACH":
            attach_volume(iqn, portal)
        elif ctype == "DETACH":
            detach_volume(iqn, portal)
        else:
            raise ValueError(f"Unknown command type: {ctype}")

        client.ack_command(cid, success=True, message="OK")
        log.info("Command #%d done.", cid)

    except Exception as e:
        log.error("Command #%d failed: %s", cid, e)
        client.ack_command(cid, success=False, message=str(e))


# ── Subprocess helper ─────────────────────────────────────────────────────────

def _run(cmd: list, check=True) -> subprocess.CompletedProcess:
    result = subprocess.run(cmd, capture_output=True, text=True)
    if check and result.returncode != 0:
        raise RuntimeError(
            f"Command failed: {' '.join(cmd)}\n"
            f"stdout: {result.stdout}\nstderr: {result.stderr}"
        )
    return result


# ── Entry point ───────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(prog="staas-agent")
    sub    = parser.add_subparsers(dest="command", required=True)

    p = sub.add_parser("register", help="Register this machine with STaaS")
    p.add_argument("--server", required=True, help="STaaS API base URL")
    p.add_argument("--token",  required=True, help="Your STaaS login JWT")
    p.set_defaults(func=cmd_register)

    p = sub.add_parser("start", help="Start polling for commands")
    p.set_defaults(func=cmd_start)

    p = sub.add_parser("status", help="Show registration info")
    p.set_defaults(func=cmd_status)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()