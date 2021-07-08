import { Injectable, NgZone } from '@angular/core';
import { Observable } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class SseService {
  constructor(private _zone: NgZone) {}

  getEventSource(): EventSource {
    return new EventSource('/app/updates');
  }

  getServerSentEvents(): Observable<String> {
    return new Observable(observer => {
      const eventSource = this.getEventSource();

      eventSource.onmessage = event => {
        this._zone.run(() => {
          observer.next("type = [" + event.type + "] data = [" + event.data + "]");
        });
      };

      eventSource.onerror = error => {
        this._zone.run(() => {
          observer.error(error);
        });
      };
    });
  }
}
