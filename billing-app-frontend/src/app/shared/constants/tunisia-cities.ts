export interface TunisiaCity {
  ville: string;
  codePostal: string;
  gouvernorat: string;
}

export const TUNISIA_CITIES: TunisiaCity[] = [
  // Tunis
  { ville: 'Tunis', codePostal: '1000', gouvernorat: 'Tunis' },
  { ville: 'La Marsa', codePostal: '2070', gouvernorat: 'Tunis' },
  { ville: 'Carthage', codePostal: '2016', gouvernorat: 'Tunis' },
  { ville: 'Ariana', codePostal: '2080', gouvernorat: 'Ariana' },
  { ville: 'Ben Arous', codePostal: '2013', gouvernorat: 'Ben Arous' },
  { ville: 'Manouba', codePostal: '2010', gouvernorat: 'Manouba' },
  
  // Sfax
  { ville: 'Sfax', codePostal: '3000', gouvernorat: 'Sfax' },
  { ville: 'Sfax Ville', codePostal: '3000', gouvernorat: 'Sfax' },
  { ville: 'Sakiet Ezzit', codePostal: '3021', gouvernorat: 'Sfax' },
  
  // Sousse
  { ville: 'Sousse', codePostal: '4000', gouvernorat: 'Sousse' },
  { ville: 'Hammam Sousse', codePostal: '4011', gouvernorat: 'Sousse' },
  { ville: 'Kalaa Kebira', codePostal: '4060', gouvernorat: 'Sousse' },
  
  // Monastir
  { ville: 'Monastir', codePostal: '5000', gouvernorat: 'Monastir' },
  { ville: 'Moknine', codePostal: '5050', gouvernorat: 'Monastir' },
  { ville: 'Ksar Hellal', codePostal: '5070', gouvernorat: 'Monastir' },
  
  // Nabeul
  { ville: 'Nabeul', codePostal: '8000', gouvernorat: 'Nabeul' },
  { ville: 'Hammamet', codePostal: '8050', gouvernorat: 'Nabeul' },
  { ville: 'Kelibia', codePostal: '8090', gouvernorat: 'Nabeul' },
  
  // Bizerte
  { ville: 'Bizerte', codePostal: '7000', gouvernorat: 'Bizerte' },
  { ville: 'Menzel Bourguiba', codePostal: '7050', gouvernorat: 'Bizerte' },
  
  // Kairouan
  { ville: 'Kairouan', codePostal: '3100', gouvernorat: 'Kairouan' },
  { ville: 'Haffouz', codePostal: '3130', gouvernorat: 'Kairouan' },
  
  // Gabes
  { ville: 'Gabes', codePostal: '6000', gouvernorat: 'Gabes' },
  { ville: 'Mareth', codePostal: '6020', gouvernorat: 'Gabes' },
  
  // Medenine
  { ville: 'Medenine', codePostal: '4100', gouvernorat: 'Medenine' },
  { ville: 'Djerba', codePostal: '4116', gouvernorat: 'Medenine' },
  { ville: 'Houmt Souk', codePostal: '4180', gouvernorat: 'Medenine' },
  
  // Gafsa
  { ville: 'Gafsa', codePostal: '2100', gouvernorat: 'Gafsa' },
  { ville: 'Metlaoui', codePostal: '2113', gouvernorat: 'Gafsa' },
  
  // Kasserine
  { ville: 'Kasserine', codePostal: '1200', gouvernorat: 'Kasserine' },
  { ville: 'Sbeitla', codePostal: '1250', gouvernorat: 'Kasserine' },
  
  // Sidi Bouzid
  { ville: 'Sidi Bouzid', codePostal: '9100', gouvernorat: 'Sidi Bouzid' },
  
  // Beja
  { ville: 'Beja', codePostal: '9000', gouvernorat: 'Beja' },
  
  // Jendouba
  { ville: 'Jendouba', codePostal: '8100', gouvernorat: 'Jendouba' },
  
  // Kef
  { ville: 'Kef', codePostal: '7100', gouvernorat: 'Kef' },
  
  // Siliana
  { ville: 'Siliana', codePostal: '6100', gouvernorat: 'Siliana' },
  
  // Mahdia
  { ville: 'Mahdia', codePostal: '5100', gouvernorat: 'Mahdia' },
  { ville: 'Ksour Essaf', codePostal: '5140', gouvernorat: 'Mahdia' },
  
  // Tozeur
  { ville: 'Tozeur', codePostal: '2200', gouvernorat: 'Tozeur' },
  { ville: 'Nefta', codePostal: '2240', gouvernorat: 'Tozeur' },
  
  // Kebili
  { ville: 'Kebili', codePostal: '4200', gouvernorat: 'Kebili' },
  { ville: 'Douz', codePostal: '4260', gouvernorat: 'Kebili' },
  
  // Tataouine
  { ville: 'Tataouine', codePostal: '3200', gouvernorat: 'Tataouine' },
  
  // Zaghouan
  { ville: 'Zaghouan', codePostal: '1100', gouvernorat: 'Zaghouan' }
];

export function getCodePostalByVille(ville: string): string {
  const city = TUNISIA_CITIES.find(c => c.ville.toLowerCase() === ville.toLowerCase());
  return city ? city.codePostal : '';
}

export function getGouvernoratByVille(ville: string): string {
  const city = TUNISIA_CITIES.find(c => c.ville.toLowerCase() === ville.toLowerCase());
  return city ? city.gouvernorat : '';
}

export function getVillesByGouvernorat(gouvernorat: string): TunisiaCity[] {
  return TUNISIA_CITIES.filter(c => c.gouvernorat === gouvernorat);
}

export function getAllVilles(): string[] {
  return TUNISIA_CITIES.map(c => c.ville);
}

export function getAllGouvernorats(): string[] {
  const gouvernorats = [...new Set(TUNISIA_CITIES.map(c => c.gouvernorat))];
  return gouvernorats.sort();
}
