import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FileShares } from './file-shares';

describe('FileShares', () => {
  let component: FileShares;
  let fixture: ComponentFixture<FileShares>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FileShares]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FileShares);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
