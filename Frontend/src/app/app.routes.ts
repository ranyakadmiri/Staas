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

export const routes: Routes = [ { path: '', component: Login },
  { path: 'projects', component: Projects },
  { path: 'credentials', component: Credentials },
 { path: 'register', component: Register },
   { path: 'dashboard', component: Dashboard },
  { path: 'buckets/:projectId', component: Buckets },
  { path: 'objects/:projectId/:bucketName', component: Objects },
{ path: 'block-storage', component: BlockStorage },
{ path: 'file-storage',  component: FileStorage  },
{ path: 'iscsii-storage',  component: IscsiStorage  }];
