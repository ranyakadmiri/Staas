import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IscsiStorage } from './iscsi-storage';

describe('IscsiStorage', () => {
  let component: IscsiStorage;
  let fixture: ComponentFixture<IscsiStorage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IscsiStorage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IscsiStorage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
