import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { ref, onValue } from 'firebase/database';
import { db } from '../firebase';
import JoinScreen from './JoinScreen';
import LobbyScreen from './LobbyScreen';
import PlayScreen from './PlayScreen';

export type GameState = 'lobby' | 'role_selection' | 'night' | 'night_killer_awake' | 'morning' | 'deliberation' | 'voting' | 'game_over';

export interface Player {
  id: string;
  name: string;
  role: string;
  alive: boolean;
  votedFor?: string;
  nightActionTarget?: string;
  readyToVote?: boolean;
}

export interface GameData {
  state: GameState;
  villageName?: string;
  dayNumber?: number;
  deliberationEndTime?: number;
  votingEndTime?: number;
  winner?: string;
  recentDeath?: string;
  players?: Record<string, Player>;
}

export default function GameController() {
  const { gameId } = useParams<{ gameId: string }>();
  const [playerId, setPlayerId] = useState<string | null>(sessionStorage.getItem(`mafia_player_${gameId}`));
  const [game, setGame] = useState<GameData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!gameId) return;

    const gameRef = ref(db, `games/${gameId}`);
    const unsubscribe = onValue(gameRef, (snapshot) => {
      if (snapshot.exists()) {
        setGame(snapshot.val());
      } else {
        setGame(null);
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, [gameId]);

  // If the game doesn't exist yet, it means the host hasn't created it or invalid code
  if (loading) {
    return <div className="flex h-screen items-center justify-center text-white">Loading...</div>;
  }

  if (!game) {
    return <div className="flex h-screen items-center justify-center text-red-400">Game not found or has ended.</div>;
  }

  // Check if player is actually in the game's player list
  const isPlayerInGame = playerId && game.players && game.players[playerId];

  if (!isPlayerInGame) {
    return <JoinScreen gameId={gameId!} onJoin={(id) => {
      setPlayerId(id);
      sessionStorage.setItem(`mafia_player_${gameId}`, id);
    }} />;
  }

  const me = game.players![playerId];

  if (game.state === 'lobby' || game.state === 'role_selection') {
    return <LobbyScreen gameId={gameId!} playerId={playerId} />;
  }

  return <PlayScreen game={game} me={me} gameId={gameId!} />;
}
