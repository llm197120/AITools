// 获取所有动态页面(views目录下的所有vue文件和tsx文件)
const allFiles = import.meta.glob(
  [
    '../views/**/*.{vue,tsx}', // 获取所有 vue 和 tsx 文件
    // 排除特定文件夹
    '!../views/system/approvalrole/compoments/**',
    //update-begin---author:copilot ---date:2026-08-11 for：【P2】生产构建排除 demo 视图，减小动态页映射体积-----------
    '!../views/demo/**',
    //update-end---author:copilot ---date:2026-08-11 for：【P2】生产构建排除 demo 视图，减小动态页映射体积-----------
  ]
);
// 合并所有动态页面
export const dynamicPages = { ...allFiles };
