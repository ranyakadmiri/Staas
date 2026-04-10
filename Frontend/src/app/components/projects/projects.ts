import { ChangeDetectorRef, Component } from '@angular/core';
import { Api } from '../../services/api';
import { FormsModule, NgForm } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-projects',
  imports: [FormsModule,CommonModule ],
  templateUrl: './projects.html',
  styleUrl: './projects.css',
})
export class Projects {
  projects:any[] = [];
projectName="";
selectedProjectId=1;
project:any = {
  name:'',
  description:'',
  storageType:'OBJECT',
  region:'default',
  maxBuckets:100,
  maxStorageGB:1000
};
constructor(private api:Api ,private router:Router,private cd: ChangeDetectorRef){}

ngOnInit(){
  this.loadProjects();
}
openProject(projectId:number){
  this.router.navigate(['/buckets', projectId]);
}
loadProjects(){

 this.api.getProjects().subscribe({

   next:(res:any)=>{
     this.projects = res;
      this.cd.detectChanges(); 
   },

   error:(err)=>{
     console.error("Projects loading failed",err);
   }

 });

}

createProject(){

  this.api.createProject(this.project)
  .subscribe(()=>{
    this.loadProjects();
  });

}

}
