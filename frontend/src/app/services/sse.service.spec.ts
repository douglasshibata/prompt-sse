import { TestBed } from '@angular/core/testing';
import { SseService, PromptEvent } from './sse.service';

describe('SseService', () => {
  let service: SseService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SseService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should connect and emit events', (done) => {
    // Mock EventSource
    const mockEventSource = {
      onopen: null as any,
      onmessage: null as any,
      onerror: null as any,
      close: jasmine.createSpy('close'),
      addEventListener: jasmine.createSpy('addEventListener'),
    };
    spyOn(window, 'EventSource').and.returnValue(mockEventSource as any);

    const testEvent: PromptEvent = {
      user: 'test user',
      ai: 'test ai',
      time: '2023-01-01T00:00:00Z',
    };

    service.connect().subscribe((event) => {
      expect(event).toEqual(testEvent);
      done();
    });

    // Simulate server event
    const callback = mockEventSource.addEventListener.calls.argsFor(0)[1];
    callback({
      data: JSON.stringify(testEvent),
    });
  });

  it('should disconnect', () => {
    const mockEventSource = {
      close: jasmine.createSpy('close'),
      addEventListener: jasmine.createSpy('addEventListener'),
    } as any;
    spyOn(window, 'EventSource').and.returnValue(mockEventSource);
    service.connect().subscribe().unsubscribe(); // Triggers disconnect

    expect(mockEventSource.close).toHaveBeenCalled();
  });
});
