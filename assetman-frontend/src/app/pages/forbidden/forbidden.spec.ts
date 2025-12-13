import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ForbiddenPageComponent } from './forbidden';

describe('ForbiddenPageComponent', () => {
  let fixture: ComponentFixture<ForbiddenPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForbiddenPageComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ForbiddenPageComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });
});
