import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BlockStorage } from './block-storage';

describe('BlockStorage', () => {
  let component: BlockStorage;
  let fixture: ComponentFixture<BlockStorage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BlockStorage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BlockStorage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
