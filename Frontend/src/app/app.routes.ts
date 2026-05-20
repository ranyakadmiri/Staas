import { Routes } from '@angular/router';
import { Login } from './components/login/login';
import { Projects } from './components/projects/projects';
import { Buckets } from './components/buckets/buckets';
import { Objects } from './components/objects/objects';
import { Dashboard } from './components/dashboard/dashboard';
import { Register } from './components/register/register';
import { Credentials } from './components/credentials/credentials';
import { BlockStorage } from './components/block-storage/block-storage';
import { FileStorage } from './components/file-storage/file-storage';
import { IscsiStorage } from './components/iscsi-storage/iscsi-storage';
import { FileShares } from './components/file-shares/file-shares';
import { AdminUsers } from './components/admin-users/admin-users';
import { Billing } from './components/billing/billing';

export const routes: Routes = [ { path: '', component: Login },
  { path: 'projects', component: Projects },
  { path: 'credentials', component: Credentials },
 { path: 'register', component: Register },
   { path: 'dashboard', component: Dashboard },
  { path: 'buckets/:projectId', component: Buckets },
  { path: 'objects/:projectId/:bucketName', component: Objects },
{ path: 'block-storage', component: BlockStorage },
{ path: 'file-storage',  component: FileStorage  },
{ path: 'iscsii-storage/:projectId',  component: IscsiStorage  },
{ path: 'file-shares/:projectId',  component: FileShares },
{ path: 'admin',  component: AdminUsers },
{ path: 'billing/:projectId', component: Billing }

];
