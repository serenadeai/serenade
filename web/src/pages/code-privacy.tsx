import React from "react";
import { Link } from "gatsby";
import { Legal } from "../components/pages";

const CodePrivacyPage = () => (
  <Legal title="Code Privacy">
    <p>
      Serenade is a freely available, open-source service. You can help improve Serenade by opting
      in to anonymously sharing: (1) audio data and (2) code data. This data has a huge impact on
      making Serenade more accurate and more powerful!
    </p>
    <p>
      Serenade does not collect your name, email, or other contact information. Instead, when
      Serenade is installed, a unique, random string will be generated and used as an identifier if
      you opt into sharing data with Serenade. If you reinstall Serenade or install Serenade on
      another device, a new random string will be generated.
    </p>
    <p>
      When using Serenade with a cloud endpoint, your audio data and the file you are currently
      editing are sent to Serenade servers, which allows Serenade's speech-to-code engine to give
      you the most accurate response for your code (e.g., by taking into account the code styling
      conventions in your file). Your audio and code data is never stored or persisted to disk
      unless you've opted into sharing it, and you can change your settings at any time from the
      Serenade application. All data is used exclusively to improve Serenade's speech-to-code
      engine.
    </p>
    <p>
      When using Serenade locally, all processing happens on-device, so no audio or code data leaves
      your device unless you've opted in to sharing it. To run Serenade locally, simply open the
      Serenade settings menu and select Server > Local.
    </p>
    <h2>Audio Data</h2>
    <p>
      You can opt into sharing your audio data with Serenade in order to improve Serenade's speech
      engine. If you choose to share your audio data, then the audio from your voice commands will
      be stored and used to improve Serenade's machine learning models.
    </p>
    <h2>Code Data</h2>
    <p>
      You can opt into sharing your code data with Serenade in order to improve Serenade's natural
      language to code engine. If you choose to share your code data, then the file you are
      currently editing, filename, and editor name will be stored along with the response that was
      generated from our speech-to-code engine.
    </p>
  </Legal>
);

export default CodePrivacyPage;
