import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app';
import { appConfig } from './app/app.config';
import { AuthService } from './app/core/auth/auth.service';

bootstrapApplication(AppComponent, appConfig)
  .then(appRef => {
    const auth = appRef.injector.get(AuthService);
    auth.ensureSessionOnce().subscribe({ error: () => {} });
  })
  .catch(err => console.error(err));
