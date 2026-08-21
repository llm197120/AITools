import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.homeai.app',
  appName: '家庭AI小工具',
  webDir: 'dist/build/h5',
  backgroundColor: '#F3F2EE',
  server: {
    androidScheme: 'https',
    cleartext: true,
  },
  android: {
    allowMixedContent: true,
    backgroundColor: '#F3F2EE',
  },
  plugins: {
    LocalNotifications: {
      smallIcon: 'ic_stat_homeai',
      iconColor: '#1B4F8A',
    },
  },
}

export default config
