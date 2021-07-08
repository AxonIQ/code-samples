import { Component, OnInit } from '@angular/core';
import { SseService } from "./app.sse.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  constructor(private _sseService: SseService) {  }
  messages: String[] = []

  ngOnInit(): void {
    this._sseService
      .getServerSentEvents()
      .subscribe(data => this.messages.push(data));
  }
}
