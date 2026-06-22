import { useState } from 'react';
import { ref, update } from 'firebase/database';
import { db } from '../firebase';
import type { GameData, Player } from './GameController';

interface PlayScreenProps {
  game: GameData;
  me: Player;
  gameId: string;
}

export default function PlayScreen({ game, me, gameId }: PlayScreenProps) {
  const [selectedTarget, setSelectedTarget] = useState<string | null>(null);

  const handleNightAction = async () => {
    if (!selectedTarget) return;
    try {
      await update(ref(db, `games/${gameId}/players/${me.id}`), {
        nightActionTarget: selectedTarget
      });
    } catch (e) {
      console.error(e);
    }
  };

  // Ensure we don't crash if players map is somehow missing
  const allPlayers = Object.values(game.players || {});
  const aliveOtherPlayers = allPlayers.filter(p => p.id !== me.id && p.alive);

  if (!me.alive) {
    return (
      <div className="flex h-screen flex-col items-center justify-center bg-black px-6 text-center">
        <h1 className="text-6xl font-bold text-red-600 mb-4 animate-pulse">YOU ARE DEAD</h1>
        <p className="text-gray-400">Wait for the game to finish to see who wins.</p>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col items-center bg-gray-900 px-6 py-12 text-white">
      {/* Header Info */}
      <div className="w-full max-w-md flex justify-between items-center border-b border-gray-700 pb-4 mb-8">
        <div>
          <p className="text-sm text-gray-400">Day {game.dayNumber || 1}</p>
          <p className="text-xl font-bold">{game.villageName}</p>
        </div>
        <div className="text-right">
          <p className="text-sm text-gray-400">Your Role</p>
          <p className={`text-xl font-bold uppercase ${me.role === 'killer' ? 'text-red-500' : 'text-blue-400'}`}>
            {me.role}
          </p>
        </div>
      </div>

      {/* State specific UI */}
      {(game.state === 'night' || game.state === 'night_killer_awake') && (
        <div className="w-full max-w-md flex flex-col items-center">
          <h2 className="text-3xl font-bold mb-2">It is Night</h2>
          
          {me.role === 'killer' && game.state === 'night_killer_awake' ? (
            me.nightActionTarget ? (
              <p className="text-gray-400 mt-12 text-center text-lg italic">You have made your choice. Go back to sleep.</p>
            ) : (
              <div className="w-full mt-8">
                <p className="text-red-400 mb-6 text-center font-medium">Choose who to eliminate:</p>
                <div className="flex flex-col gap-3">
                  {aliveOtherPlayers.map(p => (
                    <button
                      key={p.id}
                      onClick={() => setSelectedTarget(p.id)}
                      className={`p-4 rounded-xl text-left font-medium transition-colors border ${
                        selectedTarget === p.id 
                          ? 'bg-red-900 border-red-500 text-white' 
                          : 'bg-gray-800 border-gray-700 text-gray-300 hover:bg-gray-700'
                      }`}
                    >
                      {p.name}
                    </button>
                  ))}
                </div>
                <button
                  onClick={handleNightAction}
                  disabled={!selectedTarget}
                  className="w-full mt-8 bg-red-600 disabled:bg-gray-700 disabled:text-gray-500 text-white p-4 rounded-xl font-bold text-lg hover:bg-red-700 transition"
                >
                  CONFIRM KILL
                </button>
              </div>
            )
          ) : (
            <p className="text-gray-500 mt-12 text-center text-xl italic animate-pulse">Go to sleep. Close your eyes.</p>
          )}
        </div>
      )}

      {game.state === 'morning' && (
        <div className="w-full max-w-md flex flex-col items-center text-center">
          <h2 className="text-3xl font-bold mb-6 text-yellow-500">Morning has broken</h2>
          
          <div className="bg-gray-800 p-6 rounded-2xl w-full border border-gray-700 mb-8">
            {game.recentDeath ? (
              <>
                <p className="text-xl mb-2">The village wakes up to find a tragic scene...</p>
                <p className="text-2xl font-bold text-red-500">{game.players![game.recentDeath].name} was killed!</p>
              </>
            ) : (
              <p className="text-xl text-green-400 font-bold">Miraculously, nobody died last night!</p>
            )}
          </div>

          <p className="text-gray-300 mb-8">Discuss amongst yourselves. Who is acting suspiciously?</p>

          {me.readyToVote ? (
            <p className="text-green-400 font-bold mt-4 italic">You are ready to vote. Waiting for others...</p>
          ) : (
            <button
              onClick={async () => {
                await update(ref(db, `games/${gameId}/players/${me.id}`), { readyToVote: true });
              }}
              className="w-full bg-yellow-600 text-white p-4 rounded-xl font-bold text-lg hover:bg-yellow-700 transition"
            >
              I'M READY TO VOTE
            </button>
          )}
        </div>
      )}

      {/* Voting Phase UI */}
      {game.state === 'voting' && (
        <div className="w-full max-w-md flex flex-col items-center">
          <h2 className="text-3xl font-bold mb-2 text-red-500">Voting Time</h2>
          <p className="text-gray-300 mb-8 text-center">Who do you want to eliminate?</p>

          {me.votedFor ? (
            <p className="text-gray-400 mt-4 italic text-center">You have cast your vote. Waiting for others...</p>
          ) : (
            <div className="w-full flex flex-col gap-3">
              {aliveOtherPlayers.map(p => (
                <button
                  key={p.id}
                  onClick={async () => {
                    await update(ref(db, `games/${gameId}/players/${me.id}`), { votedFor: p.id });
                  }}
                  className="p-4 rounded-xl text-left font-medium transition-colors border bg-gray-800 border-gray-700 text-gray-300 hover:bg-gray-700 focus:bg-red-900 focus:border-red-500"
                >
                  Vote for {p.name}
                </button>
              ))}
              <button
                onClick={async () => {
                  await update(ref(db, `games/${gameId}/players/${me.id}`), { votedFor: 'skip' });
                }}
                className="p-4 rounded-xl text-center font-bold transition-colors border bg-gray-900 border-gray-600 text-gray-400 hover:bg-gray-800 mt-4"
              >
                Skip Vote
              </button>
            </div>
          )}
        </div>
      )}

      {/* Game Over Phase UI */}
      {game.state === 'game_over' && (
        <div className="w-full max-w-md flex flex-col items-center">
          <h2 className="text-5xl font-bold mb-6 text-white animate-pulse">GAME OVER</h2>
          <div className={`p-8 rounded-2xl w-full border text-center mb-8 ${game.winner === 'killers' ? 'bg-red-900 border-red-500' : 'bg-blue-900 border-blue-500'}`}>
            <p className="text-2xl font-bold text-white mb-2">
              {game.winner === 'killers' ? 'The Killers Win!' : 'The Peasants Win!'}
            </p>
            <p className="text-gray-300">
              {game.winner === 'killers' ? 'The village has fallen to darkness.' : 'The village is finally safe.'}
            </p>
          </div>
          
          <div className="w-full bg-gray-800 p-6 rounded-2xl border border-gray-700">
            <h3 className="text-xl font-bold mb-4 text-center">Final Roles</h3>
            <div className="flex flex-col gap-2">
              {allPlayers.map(p => (
                <div key={p.id} className="flex justify-between items-center bg-gray-900 p-3 rounded-xl">
                  <span className={`font-medium ${p.alive ? 'text-white' : 'text-gray-500 line-through'}`}>
                    {p.name} {p.id === me.id ? '(You)' : ''}
                  </span>
                  <span className={`uppercase font-bold text-sm ${p.role === 'killer' ? 'text-red-500' : 'text-blue-400'}`}>
                    {p.role}
                  </span>
                </div>
              ))}
            </div>
          </div>
          
          <p className="text-gray-400 mt-8">Waiting for the host to start a new game...</p>
        </div>
      )}

      {/* Other States */}
      {game.state !== 'night' && game.state !== 'night_killer_awake' && game.state !== 'morning' && game.state !== 'voting' && game.state !== 'game_over' && (
        <div className="text-center mt-12">
          <p className="text-2xl text-gray-400">Phase: {game.state}</p>
        </div>
      )}
    </div>
  );
}
