import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

interface TeamMember {
  id: number;
  nom: string;
  role: string;
  email: string;
  telephone: string;
  status: 'ACTIVE' | 'ABSENT' | 'CONGE';
  ventesJour: number;
  ventesMois: number;
}

@Component({
  selector: 'app-boutique-team',
  templateUrl: './boutique-team.component.html',
  styleUrls: ['./boutique-team.component.css']
})
export class BoutiqueTeamComponent implements OnInit {
  boutiqueId = 1;
  members: TeamMember[] = [];
  loading = false;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    const storedId = this.authService.getBoutiqueId();
    this.boutiqueId = storedId ? parseInt(storedId, 10) : 1;
    this.loadTeam();
  }

  loadTeam(): void {
    // Sample data - in a real scenario, this should call a team endpoint
    this.members = [
      {
        id: 2, nom: 'Karim Bouaziz', role: 'Agent Commercial',
        email: 'k.bouaziz@telecom.tn', telephone: '+216 20 111 222',
        status: 'ACTIVE', ventesJour: 3, ventesMois: 28
      },
      {
        id: 4, nom: 'Sami Jouini', role: 'Agent Commercial',
        email: 's.jouini@telecom.tn', telephone: '+216 20 333 444',
        status: 'ACTIVE', ventesJour: 2, ventesMois: 22
      },
      {
        id: 5, nom: 'Nadia Khelifi', role: 'Agent Commercial',
        email: 'n.khelifi@telecom.tn', telephone: '+216 20 555 666',
        status: 'CONGE', ventesJour: 0, ventesMois: 18
      }
    ];
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'member-active';
      case 'ABSENT': return 'member-absent';
      case 'CONGE': return 'member-conge';
      default: return '';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'En poste';
      case 'ABSENT': return 'Absent';
      case 'CONGE': return 'En congé';
      default: return status;
    }
  }

  getInitials(name: string): string {
    return name.split(' ').map(p => p.charAt(0)).join('').toUpperCase().substring(0, 2);
  }

  get activeCount(): number {
    return this.members.filter(m => m.status === 'ACTIVE').length;
  }

  get totalSalesToday(): number {
    return this.members.reduce((sum, m) => sum + m.ventesJour, 0);
  }
}
