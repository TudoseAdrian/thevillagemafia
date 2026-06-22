import { ref, remove } from 'firebase/database';
import { db } from '../firebase';
import { LogOut } from 'lucide-react';

interface LobbyScreenProps {
  gameId: string;
  playerId: string;
}

export default function LobbyScreen({ gameId, playerId }: LobbyScreenProps) {
  const handleLeave = async () => {
    try {
      await remove(ref(db, `games/${gameId}/players/${playerId}`));
      localStorage.removeItem(`mafia_player_${gameId}`);
      window.location.reload();
    } catch (error) {
      console.error("Failed to leave:", error);
    }
  };

  return (
    <div className="flex h-screen flex-col items-center justify-center bg-gray-900 px-6 relative">
      <button 
        onClick={handleLeave}
        className="absolute top-6 right-6 flex items-center gap-2 rounded-lg bg-gray-800 px-4 py-2 text-sm text-gray-400 transition hover:bg-gray-700 hover:text-white"
      >
        <LogOut size={16} />
        Leave
      </button>

      <div className="flex flex-col items-center animate-pulse">
        <div className="h-16 w-16 mb-6 rounded-full border-4 border-red-600 border-t-transparent animate-spin"></div>
        <h2 className="text-2xl font-medium text-white tracking-widest">GAME PREPARING...</h2>
        <p className="mt-4 text-gray-500 text-center max-w-xs">Waiting for the host to assign roles and start the session.</p>
      </div>
    </div>
  );
}
