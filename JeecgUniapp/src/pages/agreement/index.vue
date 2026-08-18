<route lang="json5">
{
  style: {
    navigationBarTitleText: '用户协议',
    navigationBarBackgroundColor: '#F3F2EE',
  },
}
</route>

<template>
  <view class="hai-page">
    <view class="hai-card doc-card">
      <text class="doc-title">用户协议</text>
      <text class="doc-update">更新日期：2026 年 8 月 17 日</text>
      <text class="doc-intro">
        欢迎使用「家庭AI小工具」。在使用本应用前，请您仔细阅读并充分理解本协议的全部内容。您点击同意或开始使用本应用，即视为您已阅读并同意本协议。
      </text>

      <view class="doc-section">
        <text class="doc-h2">一、服务条款</text>
        <text class="doc-p">1. 本应用为家庭场景提供 AI 对话、家庭管理、菜谱、账单、学习资料、资料存储等服务；</text>
        <text class="doc-p">2. 我们有权根据业务发展需要，对服务内容进行增加、调整或下线，并尽可能提前通知；</text>
        <text class="doc-p">3. 您应妥善保管账号及密码，因您保管不善造成的损失由您自行承担；</text>
        <text class="doc-p">4. 我们可能因系统维护、升级等原因暂停部分服务，将尽力减少对您的影响。</text>
      </view>

      <view class="doc-section">
        <text class="doc-h2">二、账号规则</text>
        <text class="doc-p">1. 您需使用真实、合法的手机号或微信账号注册，不得冒用他人身份；</text>
        <text class="doc-p">2. 一个账号仅限本人使用，不得转让、出借或出售账号；</text>
        <text class="doc-p">3. 您应对账号下的所有行为负责，不得利用本应用从事违法违规活动；</text>
        <text class="doc-p">4. 如发现账号存在异常或被盗用，请及时联系我们处理。</text>
      </view>

      <view class="doc-section">
        <text class="doc-h2">三、免责声明</text>
        <text class="doc-p">1. 本应用提供的 AI 生成内容仅供参考，不构成任何专业建议，请结合实际情况判断使用；</text>
        <text class="doc-p">2. 因不可抗力、网络故障、第三方服务异常等非我方原因导致的服务中断或数据损失，我们不承担相应责任；</text>
        <text class="doc-p">3. 您在使用第三方服务（如微信登录、云存储）时，应同时遵守第三方相关协议与规则。</text>
      </view>

      <view class="doc-section">
        <text class="doc-h2">四、协议变更与联系我们</text>
        <text class="doc-p">我们可能适时修订本协议，修订后将通过应用内公告等方式通知您。如您对本协议有任何疑问，可通过邮箱 privacy@homeai.example.com 与我们联系。</text>
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

.doc-p {
  display: block;
  font-size: 25rpx;
  line-height: 1.7;
  color: var(--hai-text-secondary);
  margin-top: 8rpx;
}
</style>