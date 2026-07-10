import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PromptService } from './prompt.service';

describe('PromptService', () => {
  let service: PromptService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PromptService],
    });
    service = TestBed.inject(PromptService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should send prompt with session ID', () => {
    const prompt = 'Hello';
    const sessionId = 'test-session';

    service.sendPrompt(prompt, sessionId).subscribe();

    const req = httpMock.expectOne('/api/prompt');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ prompt });
    expect(req.request.headers.get('X-Session-Id')).toBe(sessionId);

    req.flush({});
  });
});
