import { get, post, put, del } from './request'

export const aiApi = {
  conversations: () => get<any[]>('/ai/conversations/mine'),
  createConversation: (title?: string) => post('/ai/conversations', { data: { title: title || '新对话' } }),
  renameConversation: (id: string, title: string) =>
    put(`/ai/conversations/${id}/rename`, { params: { title } }),
  deleteConversation: (id: string) => del(`/ai/conversations/${id}`),
  messages: (conversationId: string) => get<any[]>(`/ai/conversations/${conversationId}/messages`),
  stopGeneration: (conversationId: string) =>
    post('/ai/chat/stop', { params: { conversationId } }),
  /** @deprecated 兼容旧接口；新代码请用 quotaPrecheck */
  quotaCheck: (content?: string) =>
    get<any>('/ai/chat/quota', content ? { content } : undefined),
  /** R25：按场景统一预检 */
  quotaPrecheck: (scene = 'chat', text?: string) => {
    const q: Record<string, string> = { scene }
    if (text) q.text = text
    return get<any>('/ai/quota/precheck', q)
  },
}
