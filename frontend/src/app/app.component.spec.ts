import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { AppComponent } from './app.component';
import { SseService, PromptEvent } from './services/sse.service';
import { PromptService } from './services/prompt.service';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let sseService: jasmine.SpyObj<SseService>;
  let promptService: jasmine.SpyObj<PromptService>;

  const mockEvent: PromptEvent = {
    user: 'test user',
    ai: 'test ai',
    time: '2023-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    const sseSpy = jasmine.createSpyObj('SseService', ['connect', 'disconnect']);
    const promptSpy = jasmine.createSpyObj('PromptService', ['sendPrompt']);

    await TestBed.configureTestingModule({
      declarations: [AppComponent],
      imports: [FormsModule],
      providers: [
        { provide: SseService, useValue: sseSpy },
        { provide: PromptService, useValue: promptSpy },
      ],
    }).compileComponents();

    sseService = TestBed.inject(SseService) as jasmine.SpyObj<SseService>;
    promptService = TestBed.inject(PromptService) as jasmine.SpyObj<PromptService>;

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it('should connect to SSE and receive events', () => {
    sseService.connect.and.returnValue(of(mockEvent));

    component.connect();

    expect(sseService.connect).toHaveBeenCalled();
    expect(component.connected).toBeTrue();
    expect(component.events).toContain(mockEvent);
  });

  it('should disconnect from SSE', () => {
    sseService.connect.and.returnValue(of(mockEvent));
    component.connect();

    component.disconnect();

    expect(sseService.disconnect).toHaveBeenCalled();
    expect(component.connected).toBeFalse();
  });

  it('should send prompt', () => {
    promptService.sendPrompt.and.returnValue(of({}));
    component.userInput = 'Hello';
    component.sessionId = 'test-session';

    component.sendPrompt();

    expect(promptService.sendPrompt).toHaveBeenCalledWith('Hello', 'test-session');
    expect(component.userInput).toBe('');
  });

  it('should not send empty prompt', () => {
    component.userInput = '   ';
    component.sendPrompt();
    expect(promptService.sendPrompt).not.toHaveBeenCalled();
  });

  it('should handle SSE error', () => {
    sseService.connect.and.returnValue(throwError(() => new Error('SSE error')));

    component.connect();

    expect(component.connected).toBeFalse();
  });
});
