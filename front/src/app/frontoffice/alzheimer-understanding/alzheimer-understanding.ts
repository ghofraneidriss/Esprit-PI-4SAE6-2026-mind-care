import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

export interface AlzheimerSection {
  id: string;
  label: string;
  route: string;
  kicker: string;
  title: string;
  subtitle: string;
  image: string;
}

@Component({
  selector: 'app-alzheimer-understanding',
  standalone: false,
  templateUrl: './alzheimer-understanding.html',
  styleUrls: ['./alzheimer-understanding.css'],
})
export class AlzheimerUnderstandingFrontPage implements OnInit {

  activeSection: AlzheimerSection | null = null;

  readonly sections: AlzheimerSection[] = [
    {
      id: 'comprendre',
      label: 'Comprendre la maladie',
      route: '/alzheimer/comprendre-maladie',
      kicker: 'Comprendre',
      title: "COMPRENDRE LA MALADIE D'ALZHEIMER",
      subtitle: 'Définition',
      image: 'https://images.unsplash.com/photo-1559757175-5700dde675bc?w=800&q=80',
    },
    {
      id: 'decouverte',
      label: 'Découverte de la maladie',
      route: '/alzheimer/decouverte',
      kicker: 'Histoire',
      title: "DÉCOUVERTE DE LA MALADIE D'ALZHEIMER",
      subtitle: 'Alois Alzheimer, 1906',
      image: 'https://images.unsplash.com/photo-1576091160550-2173dba999ef?w=800&q=80',
    },
    {
      id: 'chiffres',
      label: 'Alzheimer en chiffres',
      route: '/alzheimer/chiffres',
      kicker: 'Épidémiologie',
      title: 'ALZHEIMER EN CHIFFRES',
      subtitle: 'Une maladie mondiale',
      image: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=800&q=80',
    },
    {
      id: 'stades',
      label: 'Les stades',
      route: '/alzheimer/stades',
      kicker: 'Évolution',
      title: 'LES STADES DE LA MALADIE',
      subtitle: 'Une progression en 3 phases',
      image: 'https://images.unsplash.com/photo-1559757148-5c350d0d3c56?w=800&q=80',
    },
    {
      id: 'symptomes',
      label: 'Les symptômes',
      route: '/alzheimer/symptomes',
      kicker: 'Signes cliniques',
      title: "LES SYMPTÔMES D'ALZHEIMER",
      subtitle: '10 signes à reconnaître',
      image: 'https://images.unsplash.com/photo-1582750433449-648ed127bb54?w=800&q=80',
    },
    {
      id: 'causes',
      label: 'Les causes',
      route: '/alzheimer/causes',
      kicker: 'Mécanismes biologiques',
      title: 'LES CAUSES DE LA MALADIE',
      subtitle: 'Plaques et enchevêtrements',
      image: 'https://images.unsplash.com/photo-1530026405186-ed1f139313f8?w=800&q=80',
    },
    {
      id: 'diagnostic',
      label: 'Le diagnostic',
      route: '/alzheimer/diagnostic',
      kicker: 'Évaluation médicale',
      title: "LE DIAGNOSTIC D'ALZHEIMER",
      subtitle: 'Comment est-il posé ?',
      image: 'https://images.unsplash.com/photo-1631815588090-d4bfec5b1ccb?w=800&q=80',
    },
    {
      id: 'traitements',
      label: 'Les traitements',
      route: '/alzheimer/traitements',
      kicker: 'Prise en charge',
      title: 'LES TRAITEMENTS DISPONIBLES',
      subtitle: 'Médicamenteux et non médicamenteux',
      image: 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=800&q=80',
    },
    {
      id: 'jeunes',
      label: 'Alzheimer chez les jeunes',
      route: '/alzheimer/jeunes',
      kicker: 'Forme précoce',
      title: 'ALZHEIMER CHEZ LES JEUNES',
      subtitle: 'La forme précoce avant 65 ans',
      image: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&q=80',
    },
    {
      id: 'hereditaire',
      label: 'La forme héréditaire',
      route: '/alzheimer/hereditaire',
      kicker: 'Génétique',
      title: 'LA FORME HÉRÉDITAIRE',
      subtitle: 'Gènes et transmission',
      image: 'https://images.unsplash.com/photo-1628595351029-c2bf17511435?w=800&q=80',
    },
    {
      id: 'fin-de-vie',
      label: 'La fin de vie',
      route: '/alzheimer/fin-de-vie',
      kicker: 'Accompagnement',
      title: 'LA FIN DE VIE AVEC ALZHEIMER',
      subtitle: 'Accompagner avec dignité',
      image: 'https://images.unsplash.com/photo-1576765608535-5f04d1e3f289?w=800&q=80',
    },
    {
      id: 'glossaire',
      label: 'Glossaire',
      route: '/alzheimer/glossaire',
      kicker: 'Vocabulaire',
      title: 'GLOSSAIRE ALZHEIMER',
      subtitle: 'Les termes essentiels',
      image: 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&q=80',
    },
  ];

  readonly symptoms = [
    { icon: '🧠', title: 'Perte de mémoire', desc: "Oubli d'événements récents, répétition des mêmes questions, perte d'objets." },
    { icon: '💬', title: 'Difficultés de langage', desc: 'Mal à trouver ses mots, phrases incomplètes, vocabulaire appauvri.' },
    { icon: '🗺️', title: 'Désorientation', desc: "Se perdre dans un lieu familier, oublier la date, la saison ou l'année." },
    { icon: '🔢', title: 'Troubles du jugement', desc: 'Difficultés à résoudre des problèmes simples, à gérer ses finances.' },
    { icon: '🍳', title: 'Tâches familières difficiles', desc: 'Oublier comment cuisiner un plat habituel, utiliser un appareil connu.' },
    { icon: '👁️', title: 'Problèmes visuels', desc: 'Difficultés à lire, à évaluer les distances, à distinguer les couleurs.' },
    { icon: '😔', title: "Changements d'humeur", desc: 'Dépression, anxiété, méfiance, irritabilité sans raison apparente.' },
    { icon: '🚶', title: 'Retrait social', desc: "Abandon des activités habituelles, isolement progressif, perte d'initiative." },
    { icon: '🔄', title: 'Comportements répétitifs', desc: 'Répéter les mêmes gestes, poser les mêmes questions, raconter les mêmes histoires.' },
    { icon: '🌙', title: 'Troubles du sommeil', desc: 'Inversion du rythme jour/nuit, agitation nocturne, somnolence diurne.' },
  ];

  readonly diagnosticSteps = [
    { title: 'Consultation médicale', desc: 'Entretien avec le médecin généraliste ou spécialiste (neurologue, gériatre). Recueil des antécédents médicaux et familiaux.' },
    { title: 'Tests neuropsychologiques', desc: "MMSE (Mini Mental State Examination), MoCA, test de l'horloge. Évaluation de la mémoire, du langage, de l'attention et des fonctions exécutives." },
    { title: 'Bilan biologique', desc: "Prise de sang pour éliminer d'autres causes de troubles cognitifs (hypothyroïdie, carence en vitamine B12, etc.)." },
    { title: 'Imagerie cérébrale', desc: "IRM ou scanner cérébral pour visualiser les structures du cerveau et détecter une atrophie ou d'autres anomalies." },
    { title: 'Biomarqueurs (si nécessaire)', desc: 'Ponction lombaire pour mesurer les protéines amyloïde et tau dans le liquide céphalorachidien. TEP-scan pour détecter les plaques amyloïdes.' },
  ];

  readonly glossaryTerms = [
    { word: 'Amyloïde (bêta-amyloïde)', def: "Protéine anormale qui s'accumule en plaques entre les neurones dans le cerveau des personnes atteintes d'Alzheimer." },
    { word: 'Aidant', def: "Personne (familiale ou professionnelle) qui accompagne au quotidien une personne atteinte de la maladie d'Alzheimer." },
    { word: 'Biomarqueur', def: 'Indicateur biologique mesurable (dans le sang, le LCR ou par imagerie) permettant de détecter la maladie avant l\'apparition des symptômes.' },
    { word: 'Démence', def: 'Syndrome caractérisé par un déclin des fonctions cognitives suffisamment sévère pour interférer avec la vie quotidienne.' },
    { word: 'MCI (Mild Cognitive Impairment)', def: "Déclin cognitif léger, stade intermédiaire entre le vieillissement normal et la démence. Tous les MCI n'évoluent pas vers Alzheimer." },
    { word: 'Neurone', def: "Cellule nerveuse du cerveau. La maladie d'Alzheimer entraîne la mort progressive des neurones, réduisant le volume cérébral." },
    { word: 'Neurofibrillaire', def: "Relatif aux enchevêtrements de protéine tau à l'intérieur des neurones, l'une des deux lésions caractéristiques d'Alzheimer." },
    { word: 'Plaques séniles', def: 'Dépôts de protéine bêta-amyloïde entre les neurones. Avec les enchevêtrements tau, elles constituent les marqueurs biologiques de la maladie.' },
    { word: 'Soins palliatifs', def: 'Soins visant à améliorer la qualité de vie des patients en fin de vie, en soulageant la douleur et en apportant un soutien psychologique.' },
    { word: 'Synapse', def: 'Zone de contact entre deux neurones permettant la transmission des signaux nerveux. Les plaques amyloïdes perturbent le fonctionnement des synapses.' },
    { word: 'Tau', def: "Protéine qui, lorsqu'elle est anormalement modifiée (hyperphosphorylée), forme des enchevêtrements à l'intérieur des neurones, causant leur mort." },
    { word: 'TEP-scan', def: 'Tomographie par émission de positons. Examen d\'imagerie permettant de visualiser les plaques amyloïdes et les enchevêtrements tau dans le cerveau.' },
  ];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.route.url.subscribe(() => {
      const path = this.router.url;
      this.activeSection = this.sections.find(s => s.route === path) ?? this.sections[0];
    });
  }

  navigateTo(section: AlzheimerSection): void {
    this.activeSection = section;
    this.router.navigate([section.route]);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  get nextSection(): AlzheimerSection | null {
    if (!this.activeSection) return null;
    const idx = this.sections.indexOf(this.activeSection);
    return idx < this.sections.length - 1 ? this.sections[idx + 1] : null;
  }
}
