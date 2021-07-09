import { Injectable, NgZone } from '@angular/core';
import { Observable } from "rxjs";

// Simple service providing an Observable of Strings. This Observable is filled with the events from the EventSource
// retrieved from 'http://localhost:8080/app/updates'.
@Injectable({
  providedIn: 'root'
})
export class SseService {
  constructor(private _zone: NgZone) {}

  getEventSource(): EventSource {
    // Since the angular app is being run from localhost:4200, and not localhost:8000
    // we need to point it to the correct endpoint first.
    return new EventSource('http://localhost:8080/app/updates');
  }

  getServerSentEvents(): Observable<String> {
    return new Observable(observer => {
      const eventSource = this.getEventSource();

      // We can't use .onmessage = ... here, because the type of the event is not 'message'.
      // Since the type of the event is `update`, we have to use `.addEventListener()` method,
      // to specify which event type we are listening to.
      eventSource.addEventListener('update', (event: any) => {
        this._zone.run(() => {
          observer.next("type = [" + event.type + "] data = [" + event.data + "]");
        });
      });

      eventSource.onerror = error => {
        this._zone.run(() => {
          observer.error(error);
        });
      };
    });
  }
}
