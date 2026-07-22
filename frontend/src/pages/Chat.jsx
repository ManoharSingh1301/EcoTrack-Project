import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { usersApi, chatApi, API_URL, parseApiError } from '../api/api';
import { MessageCircle, Send, ArrowLeft, Circle } from 'lucide-react';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import { useToast } from '../contexts/ToastContext';

function initials(name) {
  return (name || '?').split(' ').map((s) => s[0]).slice(0, 2).join('').toUpperCase();
}

function Chat({ user }) {
  const me = user.userId;
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [connected, setConnected] = useState(false);

  const clientRef = useRef(null);
  const selectedRef = useRef(null);
  const bottomRef = useRef(null);
  const toast = useToast();

  // Load the list of people you can message.
  useEffect(() => {
    usersApi.getAllUsers()
      .then(({ data }) => setUsers(data.filter((u) => u.id !== me)))
      .catch((err) => toast.error(parseApiError(err, 'Could not load users.')))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [me]);

  // Open one STOMP connection for the lifetime of the page.
  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_URL}/ws-chat?userId=${me}`),
      reconnectDelay: 4000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/user/queue/messages', (frame) => {
          const msg = JSON.parse(frame.body);
          const partner = selectedRef.current;
          if (partner && (msg.senderId === partner.id || msg.recipientId === partner.id)) {
            setMessages((prev) => [...prev, msg]);
          }
        });
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
    });
    client.activate();
    clientRef.current = client;
    return () => { client.deactivate(); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [me]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const openConversation = async (u) => {
    setSelected(u);
    selectedRef.current = u;
    setMessages([]);
    try {
      const { data } = await chatApi.getHistory(me, u.id);
      setMessages(data);
    } catch (err) {
      toast.error(parseApiError(err, 'Could not load the conversation.'));
    }
  };

  const send = (e) => {
    e?.preventDefault();
    const text = input.trim();
    if (!text || !selected) return;
    if (!clientRef.current?.connected) {
      toast.error('Not connected — reconnecting…');
      return;
    }
    clientRef.current.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ senderId: me, recipientId: selected.id, content: text }),
    });
    setInput(''); // the server echoes the saved message back to us, which appends it
  };

  if (loading) return <Spinner label="Loading chat…" />;

  return (
    <div className="space-y-4">
      <header className="flex items-center gap-3">
        <div className="p-2.5 rounded-2xl bg-gradient-to-br from-emerald-600 to-teal-500 shadow-lg shadow-emerald-600/20">
          <MessageCircle className="w-7 h-7 text-white" />
        </div>
        <div className="flex-1">
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white">Messages</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 flex items-center gap-1.5">
            <Circle className={`w-2.5 h-2.5 ${connected ? 'fill-emerald-500 text-emerald-500' : 'fill-gray-400 text-gray-400'}`} />
            {connected ? 'Connected' : 'Connecting…'}
          </p>
        </div>
      </header>

      <div className="surface rounded-2xl overflow-hidden grid md:grid-cols-[280px_1fr] h-[70vh]">
        {/* Contacts */}
        <aside className={`border-r border-gray-200/60 dark:border-gray-700/50 overflow-y-auto ${selected ? 'hidden md:block' : 'block'}`}>
          {users.length === 0 ? (
            <p className="p-4 text-sm text-gray-500 dark:text-gray-400">No other users yet. Register a second account to chat.</p>
          ) : (
            users.map((u) => (
              <button
                key={u.id}
                onClick={() => openConversation(u)}
                className={`w-full flex items-center gap-3 p-3 text-left hover:bg-emerald-50 dark:hover:bg-gray-800 transition ${
                  selected?.id === u.id ? 'bg-emerald-50 dark:bg-gray-800' : ''
                }`}
              >
                <span className="w-10 h-10 rounded-full bg-gradient-to-br from-emerald-500 to-teal-500 text-white flex items-center justify-center text-sm font-semibold flex-shrink-0">
                  {initials(u.fullName || u.username)}
                </span>
                <span className="min-w-0">
                  <span className="block font-medium text-gray-800 dark:text-gray-100 truncate">{u.fullName || u.username}</span>
                  <span className="block text-xs text-gray-500 dark:text-gray-400 truncate">@{u.username}</span>
                </span>
              </button>
            ))
          )}
        </aside>

        {/* Conversation */}
        <section className={`flex flex-col ${selected ? 'flex' : 'hidden md:flex'}`}>
          {!selected ? (
            <div className="flex-1 flex items-center justify-center p-6">
              <EmptyState icon={MessageCircle} title="Pick a conversation" message="Select someone on the left to start chatting." />
            </div>
          ) : (
            <>
              <div className="flex items-center gap-3 p-3 border-b border-gray-200/60 dark:border-gray-700/50">
                <button className="md:hidden text-gray-500" onClick={() => { setSelected(null); selectedRef.current = null; }} aria-label="Back">
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <span className="w-9 h-9 rounded-full bg-gradient-to-br from-emerald-500 to-teal-500 text-white flex items-center justify-center text-sm font-semibold">
                  {initials(selected.fullName || selected.username)}
                </span>
                <span className="font-semibold text-gray-800 dark:text-gray-100">{selected.fullName || selected.username}</span>
              </div>

              <div className="flex-1 overflow-y-auto p-4 space-y-2">
                {messages.length === 0 && (
                  <p className="text-center text-sm text-gray-400 mt-8">No messages yet — say hello 👋</p>
                )}
                {messages.map((m, i) => {
                  const mine = m.senderId === me;
                  return (
                    <div key={m.id ?? i} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                      <div className={`max-w-[75%] px-3.5 py-2 rounded-2xl text-sm ${
                        mine
                          ? 'bg-gradient-to-r from-emerald-600 to-teal-500 text-white rounded-br-sm'
                          : 'bg-gray-100 dark:bg-gray-800 text-gray-800 dark:text-gray-100 rounded-bl-sm'
                      }`}>
                        <p className="break-words">{m.content}</p>
                        {m.timestamp && (
                          <span className={`block text-[10px] mt-1 ${mine ? 'text-emerald-100' : 'text-gray-400'}`}>
                            {new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })}
                <div ref={bottomRef} />
              </div>

              <form onSubmit={send} className="p-3 border-t border-gray-200/60 dark:border-gray-700/50 flex gap-2">
                <input
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  placeholder="Type a message…"
                  className="input flex-1"
                />
                <button type="submit" className="btn-primary px-4" aria-label="Send message">
                  <Send className="w-4 h-4" />
                </button>
              </form>
            </>
          )}
        </section>
      </div>
    </div>
  );
}

export default Chat;
