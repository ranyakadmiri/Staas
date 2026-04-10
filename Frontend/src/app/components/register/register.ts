import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../../services/api';
import { Router } from '@angular/router';

@Component({
  selector: 'app-register',
  imports: [FormsModule, CommonModule],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {

  user:any = {
    email:'',
    username:'',
    password:''
    
  };
constructor(private api:Api, private router:Router){}
  register(){

    this.api.register(this.user).subscribe({

      next:(res)=>{
        alert("User created successfully");
        this.router.navigate(['/']);
      },

      error:(err)=>{
        console.error(err);
        alert("Registration failed");
      }

    });

  }
}
