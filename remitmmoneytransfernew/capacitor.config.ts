import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.remitm.app',
  appName: 'Remitm',
  webDir: 'www',
  server: {
    androidScheme: 'https'
  }
};

export default config;
