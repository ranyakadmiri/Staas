import { ChangeDetectorRef, Component } from '@angular/core';
import { Api } from '../../services/api';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-objects',
  imports: [FormsModule , CommonModule],
  templateUrl: './objects.html',
  styleUrl: './objects.css',
})
export class Objects {


  objects: any[] = [];
  bucketStats:any = {};
  bucketName!: string;
  projectId!: number;

  selectedFile!: File;

  constructor(private api: Api, private route: ActivatedRoute,private cd: ChangeDetectorRef) {}

ngOnInit() {

 
this.route.params.subscribe(params => {

 this.projectId = params['projectId'];
 this.bucketName = params['bucketName'];

});


  console.log("Project:", this.projectId);
  console.log("Bucket:", this.bucketName);
  this.loadObjects();
  this.loadStats(); 

}
loadObjects(){

 this.api.getObjects(this.projectId, this.bucketName)
 .subscribe((res:any)=>{

   this.objects = res;
   this.cd.detectChanges(); 

 });

}
 loadStats(){

    this.api.getBucketStats(this.projectId, this.bucketName)
    .subscribe((res:any)=>{

      this.bucketStats = res;
      this.cd.detectChanges();

    });

  }

onFileSelected(event: any) {
  this.selectedFile = event.target.files[0];
}

upload() {

  if (!this.selectedFile) {
    alert("Please select a file");
    return;
  }

  this.api.uploadFile(
    this.projectId,
    this.bucketName,
    this.selectedFile
  ).subscribe({
    next: (res) => {
      console.log("Upload success", res);
      alert("File uploaded");
       this.loadObjects(); // refresh list
        this.loadStats(); 
    },
    error: (err) => {
      console.error(err);
    }
  });

}
 

  }

