import { defineUniPages } from '@uni-helper/vite-plugin-uni-pages'

export default defineUniPages({
  globalStyle: {
    navigationStyle: 'default',
    navigationBarTitleText: '家庭AI小工具',
    navigationBarBackgroundColor: '#f8f8f8',
    navigationBarTextStyle: 'white',
    backgroundColor: '#000000',
  },
  easycom: {
    autoscan: true,
    custom: {
      '^wd-(.*)': 'wot-design-uni/components/wd-$1/wd-$1.vue',
      '^(?!z-paging-refresh|z-paging-load-more)z-paging(.*)':
        'z-paging/components/z-paging$1/z-paging$1.vue',
    },
  },
  tabBar: {
    color: '#aaa',
    selectedColor: '#667eea',
    backgroundColor: '#F8F8F8',
    borderStyle: 'black',
    list: [
      {
        iconPath: 'static/tabbar/tabbar-home-2.png',
        selectedIconPath: 'static/tabbar/tabbar-home.png',
        pagePath: 'pages/homeai/index',
        text: '首页',
      },
      {
        iconPath: 'static/tabbar/tabbar-demo-2.png',
        selectedIconPath: 'static/tabbar/tabbar-demo.png',
        pagePath: 'pages/homeai/family',
        text: '家庭',
      },
      {
        iconPath: 'static/tabbar/tabbar-user-2.png',
        selectedIconPath: 'static/tabbar/tabbar-user.png',
        pagePath: 'pages/homeai/profile',
        text: '个人中心',
      },
    ],
  },
})
