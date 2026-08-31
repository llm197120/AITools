/** 结束学习计时前确认，避免误触丢掉本次时长 */
export function confirmStopLearn(): Promise<boolean> {
  return new Promise((resolve) => {
    uni.showModal({
      title: '结束学习',
      content: '结束并保存本次学习时长？',
      confirmText: '结束并保存',
      cancelText: '继续学习',
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false),
    })
  })
}
