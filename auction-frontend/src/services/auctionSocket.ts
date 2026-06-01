import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { readAuthSession } from '@/utils/authSession'

export interface AuctionSocketHandlers {
  onBid?: (payload: unknown) => void
  onState?: (payload: unknown) => void
  onNotification?: (payload: unknown) => void
  onConnect?: () => void
  onDisconnect?: () => void
}

function parseMessage(message: IMessage) {
  try {
    return JSON.parse(message.body)
  } catch {
    return message.body
  }
}

export class AuctionSocket {
  private client: Client | null = null
  private subscriptions: StompSubscription[] = []

  connect(handlers: AuctionSocketHandlers = {}) {
    const token = readAuthSession().token
    const wsBase = import.meta.env.VITE_WS_BASE_URL || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
    const endpoint = `${wsBase}/api/ws${token ? `?token=${encodeURIComponent(token)}` : ''}`

    this.client = new Client({
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => undefined,
      webSocketFactory: () => new SockJS(endpoint) as unknown as WebSocket,
      onConnect: () => {
        handlers.onConnect?.()
        if (handlers.onNotification) {
          this.subscriptions.push(
            this.client!.subscribe('/user/queue/notification', (message) => {
              handlers.onNotification?.(parseMessage(message))
            }),
          )
        }
      },
      onWebSocketClose: () => handlers.onDisconnect?.(),
      onStompError: () => handlers.onDisconnect?.(),
    })

    this.client.activate()
  }

  subscribeItem(itemId: string | number, handlers: AuctionSocketHandlers = {}) {
    const addSubscriptions = () => {
      if (!this.client?.connected) {
        return
      }
      if (handlers.onBid) {
        this.subscriptions.push(
          this.client.subscribe(`/topic/auction/${itemId}`, (message) => {
            handlers.onBid?.(parseMessage(message))
          }),
        )
      }
      if (handlers.onState) {
        this.subscriptions.push(
          this.client.subscribe(`/topic/auction/${itemId}/state`, (message) => {
            handlers.onState?.(parseMessage(message))
          }),
        )
      }
    }

    if (this.client?.connected) {
      addSubscriptions()
    } else if (this.client) {
      const originalConnect = this.client.onConnect
      this.client.onConnect = (frame) => {
        originalConnect?.(frame)
        addSubscriptions()
      }
    }

    return () => this.unsubscribeAll()
  }

  unsubscribeAll() {
    this.subscriptions.forEach((subscription) => subscription.unsubscribe())
    this.subscriptions = []
  }

  disconnect() {
    this.unsubscribeAll()
    this.client?.deactivate()
    this.client = null
  }
}
