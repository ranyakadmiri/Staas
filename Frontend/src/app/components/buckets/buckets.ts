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
createBucket(){

 this.api.createBucket(this.projectId,this.bucketName)
 .subscribe(()=>{
  this.loadBuckets();
 });

}

}
