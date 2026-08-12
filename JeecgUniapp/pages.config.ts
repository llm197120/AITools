import { defineUniPages } from '@uni-helper/vite-plugin-uni-pages'

export default defineUniPages({
  globalStyle: {
    navigationStyle: 'default',
    navigationBarTitleText: '家庭AI小工具',
    navigationBarBackgroundColor: '#F3F2EE',
    navigationBarTextStyle: 'black',
    backgroundColor: '#F3F2EE',
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
    color: '#8A857C',
    selectedColor: '#1B4F8A',
    backgroundColor: '#F3F2EE',
    borderStyle: 'white',
    list: [
      {
        iconPath: 'static/tabbar/tabbar-home-2.png',
        selectedIconPath: 'static/tabbar/tabbar-home.png',
        pagePath: 'pages/homeai/index',
        text: '首页',
      },
      {
        iconPath: 'static/tabbar/tabbar-family-2.png',
        selectedIconPath: 'static/tabbar/tabbar-family.png',
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
