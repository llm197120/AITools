<route lang="json5">
{
  style: {
    navigationBarTitleText: '隐私政策',
    navigationBarBackgroundColor: '#F3F2EE',
  },
}
</route>

<template>
  <view class="hai-page">
    <view class="hai-card doc-card">
      <text class="doc-title">隐私政策</text>
      <text class="doc-update">更新日期：2026 年 8 月 17 日</text>
      <text class="doc-intro">
        欢迎使用「家庭AI小工具」。我们深知个人信息对您的重要性，将按照法律法规要求，采取相应安全保护措施，尽力保护您的个人信息安全可控。本政策将帮助您了解我们如何收集、使用、存储和保护您的个人信息。
      </text>

      <view class="doc-section">
        <text class="doc-h2">一、我们收集哪些信息</text>
        <text class="doc-h3">1. 账号信息</text>
        <text class="doc-p">手机号：用于注册、登录及账号安全验证；</text>
        <text class="doc-p">昵称：用于在家庭内展示您的身份；</text>
        <text class="doc-p">头像：用于个人资料展示（您可自行选择是否上传）。</text>
        <text class="doc-h3">2. 家庭资料</text>
        <text class="doc-p">家庭名称、家庭成员信息、家庭相册与共享资料，用于实现家庭协作功能。</text>
        <text class="doc-h3">3. 使用数据</text>
        <text class="doc-p">对话记录、菜谱收藏、学习打卡、账单记录等使用数据，用于为您提供核心功能服务。</text>
      </view>

      <view class="doc-section">
        <text class="doc-h2">二、信息用途</text>
        <text class="doc-p">1. 提供、维护和改进我们的产品与服务；</text>
        <text class="doc-p">2. 账号注册、登录与身份验证；</text>
        <text class="doc-p">3. 向您推送家庭动态、学习提醒等服务通知；</text>
        <text class="doc-p">4. 保障账号与系统安全，防范安全风险；</text>
        <text class="doc-p">5. 在取得您同意的前提下，用于其他合法用途。</text>
      </view>

      <view class="doc-section">
        <text class="doc-h2">三、权限说明</text>
        <text class="doc-p">为向您提供相应功能，我们可能会申请以下权限：</text>
        <text class="doc-p">相机：用于拍摄照片、扫码等；</text>
        <text class="doc-p">相册：用于选择、上传图片作为头像或家庭资料；</text>
        <text class="doc-p">存储：用于保存、读取本地文件与缓存；</text>
        <text class="doc-p">网络：用于访问网络服务，实现数据同步与在线功能；</text>
        <text class="doc-p">推送通知：用于向您发送家庭动态、学习提醒等服务通知。您可以在系统设置中关闭推送权限，关闭后不影响其他功能使用。</text>
      </view>

      <view class="doc-section">
        <text class="doc-h2">四、第三方 SDK 清单</text>
        <text class="doc-p">为保障相关功能的实现与安全稳定运行，我们可能接入第三方服务，具体如下：</text>
        <text class="doc-h3">1. 阿里云 OSS</text>
        <text class="doc-p">用途：提供图片、文件等家庭资料的云端存储服务；</text>
        <text class="doc-p">收集信息：上传的文件内容及必要的设备信息；</text>
        <text class="doc-p">隐私政策：https://terms.aliyun.com/legal-agreement/terms/suit_bu1_ali_cloud/suit_bu1_ali_cloud202103011017_20637.html</text>
        <text class="doc-h3">2. DCloud 应用运行时</text>
        <text class="doc-p">用途：提供 Android 应用运行环境与本地通知能力；</text>
        <text class="doc-p">收集信息：设备型号、操作系统版本等运行所必需的基础信息。</text>
      </view>

      <view class="doc-section">
        <text class="doc-h2">五、联系方式</text>
        <text class="doc-p">如您对本隐私政策或个人信息保护有任何疑问、意见或建议，可通过以下方式与我们联系：</text>
        <text class="doc-p">邮箱：privacy@homeai.example.com</text>
        <text class="doc-p">我们将在 15 个工作日内回复您的请求。</text>
      </view>

      <view class="doc-section">
        <text class="doc-h2">六、更新日期</text>
        <text class="doc-p">本政策最近更新日期为 2026 年 8 月 17 日。我们可能适时修订本政策，修订后将通过应用内公告等方式通知您。</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
.doc-card {
  padding: 40rpx 32rpx;
}

.doc-title {
  display: block;
  font-family: var(--hai-serif);
  font-size: 40rpx;
  font-weight: 700;
  color: var(--hai-text);
}

.doc-update {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: var(--hai-text-muted);
}

.doc-intro {
  display: block;
  margin-top: 24rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: var(--hai-text-secondary);
}

.doc-section {
  margin-top: 36rpx;
}

.doc-h2 {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--hai-text);
  margin-bottom: 16rpx;
}

.doc-h3 {
  display: block;
  font-size: 27rpx;
  font-weight: 600;
  color: var(--hai-text);
  margin-top: 20rpx;
  margin-bottom: 8rpx;
}

.doc-p {
  display: block;
  font-size: 25rpx;
  line-height: 1.7;
  color: var(--hai-text-secondary);
  margin-top: 8rpx;
}
</style>