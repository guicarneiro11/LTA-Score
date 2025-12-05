# LTA Score

<div align="center">

*A platform for the League of Legends fan community in Brazil to rate professional players from LTA South and LTA North*
</div>

## Overview

**LTA Score** is an Android application that connects esports fans to the competitive League of Legends scene in the LTA South (formerly CBLOL) and LTA North (formerly LCS) leagues. The platform allows the community to express their appreciation for professional players through a continuous rating system based on their performances in official matches.

Created by fans and for fans, LTA Score does not intend to be an alternative classification system to official rankings, but rather a way to engage the community in constructive discussions about the performance of professional players and celebrate great plays.

## Screenshots and Videos

https://github.com/user-attachments/assets/5ed31fe6-0254-4efa-b7fd-19199fa4e30d

<div align="center">
<img src="https://i.imgur.com/6qNVClO.png" alt="Feed Screen" width="200"/>
<img src="https://i.imgur.com/OzLRTbn.png" alt="Matches Screen" width="200"/>
<img src="https://i.imgur.com/pED3Etb.png" alt="Rating Screen" width="200"/>
<img src="https://i.imgur.com/ZswdWWV.png" alt="Voting History" width="200"/>
<img src="https://i.imgur.com/AZiSZYf.png" alt="Evaluate players" width="200"/>
<img src="https://i.imgur.com/j9Q0PWA.png" alt="Profile Screen" width="200"/>
<img src="https://i.imgur.com/uFlyeFx.png" alt="Ranking Screen" width="200"/>
<img src="https://i.imgur.com/1Z4ZKa4.png" alt="Team Feed" width="200"/>
</div>

## Key Features

### 🏆 Rating System
- Rate players from 0-10 after each match
- Provide constructive feedback on player performances
- Contribute to a community-driven evaluation of professional athletes

### 🤝 Social Connectivity
- Friends System
  * Send and accept friend requests
  * View friends' ratings and voting history
- Team Feeds
  * Share your votes with your favorite team's community
  * Interact with team-specific vote feeds
- Social Interactions
  * Comment on votes
  * React to friends' and team ratings
  * Engage in meaningful discussions

### 📅 Comprehensive Match Management
- Detailed match calendar for LTA South and North leagues (coming soon LTA North)
- Filter matches by status:
  * Upcoming matches
  * Live matches
  * Completed matches
- Access to match VODs (coming soon)

### 📊 Ranking Systems
- Multiple ranking categories:
  * Overall player rankings
  * Team-based rankings
  * Position-specific rankings (Top, Jungle, Mid, ADC, Support)
  * Most voted players

### 👤 Personalized User Experience
- Create and customize your profile
- Select and display your favorite team
- Track personal voting history
- Manage account settings

## Technologies

The application was developed using Android development best practices, focusing on a fluid and responsive user experience:

- **Kotlin Multiplatform**: Code sharing across platforms
- **Jetpack Compose**: Modern, declarative UI
- **Firebase**: Secure and scalable backend
  - Authentication: User management
  - Firestore: Data storage
  - Functions: Secure access to Riot API
- **MVVM**: Scalable and testable architecture
- **Koin**: Dependency injection
- **Coroutines & Flow**: Efficient asynchronous programming

## Architecture

The project follows a clean architecture with clear separation of responsibilities:

```
├── api             # Integration with Riot API via Firebase Functions
├── data            # Data sources and repository implementations
│   ├── datasource  # Local and static data sources
│   └── repository  # Repository implementations
├── domain          # Business rules and domain models
│   ├── models      # Domain entities
│   ├── repository  # Repository interfaces
│   └── usecases    # Application use cases
└── ui              # User interface
    ├── auth        # Authentication (login/register)
    ├── matches     # Match listing
    ├── voting      # Rating system
    ├── summary     # Ratings summary
    ├── ranking     # Player rankings
    ├── profile     # User profile
    └── friends     # Social system
```

## Security and Integrity

LTA Score was developed with the highest security standards:

- **Protected API Key**: All access to the Riot API is done through Firebase Functions, keeping the key secure on the server
- **Robust Authentication**: Complete authentication system with Firebase Auth
- **Firestore Rules**: Security rules ensure users only access permitted data
- **Secure Local Cache**: Local storage with DataStore for better performance

## Compliance with Riot Policies

LTA Score was developed in full compliance with Riot Games' policies for third-party applications.

## How to Run the Project

1. Clone this repository
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Configure Firebase
5. Run the application on an emulator or physical device

## Contact

For questions, suggestions, or collaborations, contact:

- Email: guicarneiro.dev@gmail.com
- GitHub: [github.com/guicarneiro11](https://github.com/guicarneiro11)

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">
<p>LTA Score isn't endorsed by Riot Games and doesn't reflect the views or opinions of Riot Games or anyone officially involved in producing or managing Riot Games properties. Riot Games, and all associated properties are trademarks or registered trademarks of Riot Games, Inc.</p>
</div>
