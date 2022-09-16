type HttpMethod = 'GET' | 'POST';

declare global {
  interface Window {
    websocketHost: string;
    needsCookieConsent: boolean;
  }
}

export const hostname = window.location.hostname;
const websocketHost = window.websocketHost;
const isProd = hostname.endsWith('ganttproject.cloud');
// const isProd = true;

export class Endpoint<T> {
  constructor(readonly url: string,
              private readonly method: HttpMethod = 'GET',
              private readonly host: string = hostname,
              private readonly port: number = 80,
              private readonly processData: boolean = true,
              private readonly isSecure: boolean = isProd,
              private readonly jsonBody: boolean = false) {}

  public invoke(data: T): JQueryXHR {
    const scheme = this.isSecure ? 'https' : 'http';
    const ajaxSettings: any = {
      url: `${this.url}`,
      method: this.method,
      data,
    };

    if (this.jsonBody) {
      ajaxSettings.contentType = 'application/json';
      ajaxSettings.data = JSON.stringify(data);
    } else if (!this.processData) {
      const formData = new FormData();
      for (const key in data) {
        if (data.hasOwnProperty(key)) {
          formData.append(key, data[key] as any);
        }
      }

      ajaxSettings.cache = false;
      ajaxSettings.processData = false;
      ajaxSettings.contentType = false;
      ajaxSettings.data = formData;
    }

    return $.ajax(ajaxSettings);
  }

  public redirect(data?: T) {
    window.location.assign(this.absoluteUrl(data));
  }

  public absoluteUrl(data?: T): string {
    if (data) {
      const queryString = $.param(data);
      return `${this.url}?${queryString}`;
    } else {
      return `${this.url}`;
    }
  }
}

export class EndpointBuilder<T> {
  public method: HttpMethod = 'GET';
  public host: string = hostname;
  public port: number = 80;
  public processData: boolean = true;
  public isSecure: boolean = isProd;
  public jsonBody: boolean = false;

  constructor(private readonly url: string) {}

  public build(): Endpoint<T> {
    return new Endpoint<T>(
        this.url, this.method, this.host, this.port, this.processData, this.isSecure, this.jsonBody,
    );
  }
}

// --------------- /user/get ---------------
export interface UserGetArgs {
  id: string;
  forceCreate: boolean;
  email: string;
  displayName: string;
}
export const UserGet = new Endpoint<UserGetArgs>('/user/get', 'GET');
export const Landing = new Endpoint<void>('/', 'GET');
export const Dashboard = new Endpoint<void>('/me2', 'GET');
