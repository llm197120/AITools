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
  quotaCheck: () => get<any>('/ai/chat/quota'),
}
