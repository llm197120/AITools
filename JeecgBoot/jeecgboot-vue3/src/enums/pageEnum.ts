export enum PageEnum {
  // basic login path
  BASE_LOGIN = '/login',
  // basic home path
  BASE_HOME = '/homeai/dashboard/crossStats',
  // error page path
  ERROR_PAGE = '/exception',
  // error log page path
  ERROR_LOG_PAGE = '/error-log/list',
  // auth2登录路由路径
  OAUTH2_LOGIN_PAGE_PATH = '/oauth2-app/login',
  //文件路由
  SYS_FILES_PATH = '/file/share',
  // 邮件中的跳转地址
  TOKEN_LOGIN = '/tokenLogin'
}

/** Jeecg 演示仪表盘：隐藏「主页」后不再下发，旧 homePath / redirect 会 404 */
export function isObsoleteJeecgDashboardPath(path?: string): boolean {
  if (!path) return false;
  const p = path.split('?')[0].replace(/\/+$/, '') || '/';
  return /^\/dashboard(\/(analysis|workbench|Analysis|IndexChart|IndexBdc|IndexTask))?$/i.test(p);
}

export function resolveHomePath(homePath?: string): string {
  if (!homePath || isObsoleteJeecgDashboardPath(homePath)) {
    return PageEnum.BASE_HOME;
  }
  return homePath.startsWith('/') ? homePath : `/${homePath}`;
}
