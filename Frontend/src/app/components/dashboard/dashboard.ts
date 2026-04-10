import { Component } from '@angular/core';
import { Projects } from '../projects/projects';
import { Buckets } from '../buckets/buckets';
import { Objects } from '../objects/objects';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {

}
