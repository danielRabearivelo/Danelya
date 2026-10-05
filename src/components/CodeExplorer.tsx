import { useState } from 'react';
import { ANDROID_PROJECT_FILES } from '../data/androidProjectFiles';
import { AndroidCodeFile } from '../types';
import { Download, Copy, Check, FileCode, FolderTree, Code2 } from 'lucide-react';
import JSZip from 'jszip';

export function CodeExplorer() {
  const [selectedFile, setSelectedFile] = useState<AndroidCodeFile>(ANDROID_PROJECT_FILES[0]);
  const [copied, setCopied] = useState(false);
  const [isZipping, setIsZipping] = useState(false);
  const [stepFilter, setStepFilter] = useState<number | 'all'>(1);

  const filteredFiles =
    stepFilter === 'all'
      ? ANDROID_PROJECT_FILES
      : ANDROID_PROJECT_FILES.filter(f => f.step === stepFilter || f.step === 1);

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownloadZip = async () => {
    setIsZipping(true);
    try {
      const zip = new JSZip();
      for (const file of ANDROID_PROJECT_FILES) {
        zip.file(file.path, file.content);
      }
      // Add a quick README for Android Studio
      const readme = `# Caisse Association - Projet Android Natif

Application Android complète pour la gestion des recettes, cotisations, relances et versements d'une association.
Fonctionnement 100% hors-ligne.

## Instructions pour compiler dans Android Studio :
1. Ouvrir Android Studio (version Iguana, Jellyfish, Koala ou supérieure).
2. Cliquer sur "Open" et sélectionner ce dossier décompressé.
3. Laisser Gradle synchroniser les dépendances (AGP 8.3, Kotlin 1.9.22, Room 2.6.1, Hilt 2.50).
4. Connecter un appareil physique ou lancer un émulateur Android (minSdk 26, targetSdk 34).
5. Cliquer sur le bouton "Run" (icône verte).
`;
      zip.file('README.md', readme);

      const blob = await zip.generateAsync({ type: 'blob' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'caisse_association_android_project.zip';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (e) {
      console.error('Error generating zip:', e);
    } finally {
      setIsZipping(false);
    }
  };

  return (
    <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs flex flex-col h-[750px]">
      {/* Top action header */}
      <div className="bg-slate-900 text-white p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold">
            <Code2 className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-sm font-semibold text-white">Code Source Android Natif (Kotlin + Jetpack Compose)</h2>
            <p className="text-xs text-slate-400">Arborescence complète, Room SQLite, Hilt, MVVM, 100% hors-ligne Android 14</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleDownloadZip}
            disabled={isZipping}
            className="inline-flex items-center gap-2 px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium rounded-lg transition-colors cursor-pointer shadow-sm disabled:opacity-50"
          >
            <Download className="w-3.5 h-3.5" />
            {isZipping ? 'Création de l\'archive ZIP...' : 'Télécharger le projet complet (.ZIP)'}
          </button>
        </div>
      </div>

      {/* Step selector bar */}
      <div className="bg-slate-100/80 px-4 py-2 border-b border-slate-200 flex items-center gap-2 overflow-x-auto text-xs">
        <span className="text-slate-500 font-medium whitespace-nowrap">Filtrer par étape :</span>
        <button
          onClick={() => setStepFilter(1)}
          className={`px-2.5 py-1 rounded-md font-medium transition-colors whitespace-nowrap cursor-pointer ${
            stepFilter === 1 ? 'bg-slate-900 text-white' : 'bg-white text-slate-700 hover:bg-slate-200'
          }`}
        >
          Étape 1 : Socle + Room + Membres
        </button>
        <button
          onClick={() => setStepFilter(2)}
          className={`px-2.5 py-1 rounded-md font-medium transition-colors whitespace-nowrap cursor-pointer ${
            stepFilter === 2 ? 'bg-slate-900 text-white' : 'bg-white text-slate-700 hover:bg-slate-200'
          }`}
        >
          Étape 2 : Cotisations & Répartition
        </button>
        <button
          onClick={() => setStepFilter(3)}
          className={`px-2.5 py-1 rounded-md font-medium transition-colors whitespace-nowrap cursor-pointer ${
            stepFilter === 3 ? 'bg-slate-900 text-white' : 'bg-white text-slate-700 hover:bg-slate-200'
          }`}
        >
          Étape 3 : Autres recettes & DAOs
        </button>
        <button
          onClick={() => setStepFilter(5)}
          className={`px-2.5 py-1 rounded-md font-medium transition-colors whitespace-nowrap cursor-pointer ${
            stepFilter === 5 ? 'bg-slate-900 text-white' : 'bg-white text-slate-700 hover:bg-slate-200'
          }`}
        >
          Étape 5 : Relances & Entités
        </button>
        <button
          onClick={() => setStepFilter('all')}
          className={`px-2.5 py-1 rounded-md font-medium transition-colors whitespace-nowrap cursor-pointer ${
            stepFilter === 'all' ? 'bg-slate-900 text-white' : 'bg-white text-slate-700 hover:bg-slate-200'
          }`}
        >
          Tous les fichiers ({ANDROID_PROJECT_FILES.length})
        </button>
      </div>

      {/* Main split: File tree on left, Code viewer on right */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left: File Tree */}
        <div className="w-80 bg-slate-50 border-r border-slate-200 flex flex-col overflow-y-auto">
          <div className="p-3 border-b border-slate-200 bg-slate-100/50 flex items-center gap-1.5 text-xs font-semibold text-slate-700">
            <FolderTree className="w-4 h-4 text-slate-500" />
            <span>Fichiers du projet ({filteredFiles.length})</span>
          </div>

          <div className="p-2 space-y-1">
            {filteredFiles.map((file) => {
              const isSelected = selectedFile.path === file.path;
              return (
                <button
                  key={file.path}
                  onClick={() => setSelectedFile(file)}
                  className={`w-full text-left p-2 rounded-lg text-xs font-mono transition-colors flex items-start gap-2 cursor-pointer ${
                    isSelected
                      ? 'bg-emerald-50 text-emerald-900 font-semibold border border-emerald-200'
                      : 'text-slate-700 hover:bg-slate-200/70 border border-transparent'
                  }`}
                >
                  <FileCode className={`w-3.5 h-3.5 shrink-0 mt-0.5 ${isSelected ? 'text-emerald-600' : 'text-slate-400'}`} />
                  <div className="truncate flex-1">
                    <div className="truncate font-sans font-medium text-slate-900">{file.title}</div>
                    <div className="text-[11px] text-slate-500 truncate">{file.path}</div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Right: Code Viewer */}
        <div className="flex-1 flex flex-col bg-slate-950 text-slate-200 overflow-hidden">
          {/* File Header */}
          <div className="px-4 py-2.5 bg-slate-900 border-b border-slate-800 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              <span className="text-emerald-400 font-mono font-medium">{selectedFile.path}</span>
              <span className="text-slate-500 font-sans">({selectedFile.title})</span>
            </div>

            <button
              onClick={handleCopy}
              className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded text-xs transition-colors cursor-pointer"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? 'Copié !' : 'Copier le code'}</span>
            </button>
          </div>

          {/* Code content */}
          <div className="flex-1 overflow-auto p-4 font-mono text-xs leading-relaxed selection:bg-emerald-800 selection:text-white">
            <pre className="text-slate-300">
              <code>{selectedFile.content}</code>
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
}
