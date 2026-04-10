import { ChangeDetectorRef, Component } from '@angular/core';
import { Api } from '../../services/api';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-credentials',
  imports: [CommonModule,FormsModule],
  templateUrl: './credentials.html',
  styleUrl: './credentials.css',
})
export class Credentials {
  
constructor(
 private api:Api,
 private route:ActivatedRoute,
 private router:Router,
  private cd: ChangeDetectorRef
){}

  credentials:any[]=[];

ngOnInit(){
  this.loadCredentials();
}

loadCredentials(){
  this.api.getMyCredentials()
  .subscribe((res:any)=>{
    this.credentials = res;
    this.cd.detectChanges(); 
  });
}
}
