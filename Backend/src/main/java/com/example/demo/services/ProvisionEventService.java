package com.example.demo.services;

import com.example.demo.dto.ProvisionEventResponse;
import com.example.demo.entities.BlockVolume;
import com.example.demo.entities.BlockVolumeStatus;
import com.example.demo.entities.ProvisionEvent;
import com.example.demo.repositories.BlockVolumeRepository;
import com.example.demo.repositories.ProvisionEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProvisionEventService {

    private final ProvisionEventRepository eventRepository;
    private final BlockVolumeRepository volumeRepository;

    public void addEvent(BlockVolume volume, String step, String message, boolean success) {
        eventRepository.save(
                ProvisionEvent.builder()
                        .volumeId(volume.getId())
                        .stepName(step)
                        .message(message)
                        .success(success)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    public void updateStatus(BlockVolume volume, BlockVolumeStatus status) {
        volume.setStatus(status);
        volume.setUpdatedAt(LocalDateTime.now());
        volumeRepository.save(volume);
    }

    public void fail(BlockVolume volume, String step, String message) {
        addEvent(volume, step, message, false);
        volume.setStatus(BlockVolumeStatus.ERROR);
        volume.setErrorMessage(message);
        volume.setUpdatedAt(LocalDateTime.now());
        volumeRepository.save(volume);
    }

    public List<ProvisionEventResponse> getEvents(Long volumeId) {
        return eventRepository.findByVolumeIdOrderByCreatedAtAsc(volumeId)
                .stream()
                .map(e -> ProvisionEventResponse.builder()
                        .stepName(e.getStepName())
                        .message(e.getMessage())
                        .success(e.isSuccess())
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList();
    }
}
