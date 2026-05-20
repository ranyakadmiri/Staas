import { ChangeDetectorRef, Component } from '@angular/core';
import { Api } from '../../services/api';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-buckets',
  standalone: true,
  imports: [CommonModule,FormsModule],
  templateUrl: './buckets.html',
  styleUrl: './buckets.css',
})
export class Buckets {
  buckets:any[]=[];
bucketName="";
projectId!:number;
 // Form
  newBucket = {
    name: '',
    enableQuota: false,
    maxSizeGB: 10,
    maxObjects: 10000
  };

  creating = false;
constructor(
 private api:Api,
 private route:ActivatedRoute,
 private router:Router,
  private cd: ChangeDetectorRef
){}

ngOnInit(){

this.route.params.subscribe(params => {

   this.projectId = params['projectId'];

   if(this.projectId){
     this.loadBuckets();
   }

 });

}
openBucket(bucket:string){
  this.router.navigate(
    ['/objects', this.projectId, bucket]
  );
}
loadBuckets(){

 console.log("Loading buckets for project:", this.projectId);

 this.api.getBuckets(this.projectId)
 .subscribe((res:any)=>{

   console.log("Buckets API response:", res);

   this.buckets = res;

   this.cd.detectChanges();   // 🔴 force UI update

 });

}
 createBucket() {
    if (!this.newBucket.name.trim() || this.creating) return;
    this.creating = true;

    this.api.createBucket(this.projectId, this.newBucket.name.trim()).subscribe({
      next: (res: any) => {
        // If quota enabled AND the response includes a bucket ID, create quota
        if (this.newBucket.enableQuota && res?.id) {
          this.api.createBucketQuota(res.id, this.newBucket.maxSizeGB, this.newBucket.maxObjects)
            .subscribe({
              next: () => this.afterCreate(),
              error: () => this.afterCreate() // bucket created, quota failed silently
            });
        } else {
          this.afterCreate();
        }
      },
      error: () => {
        this.creating = false;
      }
    });
  }

  private afterCreate() {
    this.newBucket = { name: '', enableQuota: false, maxSizeGB: 10, maxObjects: 10000 };
    this.creating = false;
    this.loadBuckets();
  }
}
