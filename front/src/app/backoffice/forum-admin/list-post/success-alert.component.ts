import { Component, Input, OnInit, OnDestroy } from '@angular/core';

@Component({
  selector: 'app-success-alert',
  templateUrl: './success-alert.component.html',
  styleUrls: ['./success-alert.component.css'],
  standalone: false
})
export class SuccessAlertComponent implements OnInit, OnDestroy {
  @Input() message: string = 'Post created successfully!';
  @Input() duration: number = 2000;
  
  isVisible: boolean = false;
  private timeoutId: any;

  ngOnInit(): void {
    this.showAlert();
  }

  ngOnDestroy(): void {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
  }

  private showAlert(): void {
    this.isVisible = true;
    
    this.timeoutId = setTimeout(() => {
      this.isVisible = false;
    }, this.duration);
  }
}
