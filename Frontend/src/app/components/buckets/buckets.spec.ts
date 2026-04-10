import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Buckets } from './buckets';

describe('Buckets', () => {
  let component: Buckets;
  let fixture: ComponentFixture<Buckets>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Buckets]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Buckets);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
