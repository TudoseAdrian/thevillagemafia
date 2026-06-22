import { useState } from 'react';
import { ref, push, set } from 'firebase/database';
import { db } from '../firebase';
import { UserPlus } from 'lucide-react';

interface JoinScreenProps {
  gameId: string;
  onJoin: (playerId: string) => void;
}

export default function JoinScreen({ gameId, onJoin }: JoinScreenProps) {
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);

  const handleJoin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    setLoading(true);
    try {
      const playersRef = ref(db, `games/${gameId}/players`);
      const newPlayerRef = push(playersRef);
      
      await set(newPlayerRef, {
        id: newPlayerRef.key,
        name: name.trim(),
        role: 'unassigned',
        alive: true
      });

      onJoin(newPlayerRef.key!);
    } catch (error) {
      console.error("Error joining game:", error);
      alert("Failed to join game. Check your connection.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex h-screen flex-col items-center justify-center bg-gray-900 px-6">
      <div className="w-full max-w-sm rounded-2xl bg-gray-800 p-8 shadow-2xl border border-gray-700">
        <h1 className="mb-2 text-center text-4xl font-bold text-white tracking-tight">The Village</h1>
        <p className="mb-8 text-center text-gray-400">Join the current session</p>
        
        <form onSubmit={handleJoin} className="flex flex-col gap-4">
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Enter your name"
            className="w-full rounded-xl bg-gray-900 border border-gray-700 px-4 py-4 text-white placeholder-gray-500 focus:border-red-500 focus:outline-none focus:ring-1 focus:ring-red-500 transition-colors"
            maxLength={15}
            disabled={loading}
          />
          <button
            type="submit"
            disabled={loading || !name.trim()}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-red-600 px-4 py-4 font-bold text-white transition-colors hover:bg-red-700 disabled:bg-red-900 disabled:text-gray-400"
          >
            {loading ? 'Joining...' : (
              <>
                <UserPlus size={20} />
                Join Game
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
