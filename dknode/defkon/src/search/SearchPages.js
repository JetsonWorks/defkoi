import React from "react";

import SearchPage from "./SearchPage";
import { Capabilities, Devices, PipeConfs, } from "../grid";

export function DevicesPage(props) {
  return (
    <SearchPage {...props} enableFilter={false}>
      <Devices {...props} />
    </SearchPage>
  );
}

export function CapabilitiesPage(props) {
  return (
    <SearchPage {...props}>
      <Capabilities {...props} />
    </SearchPage>
  );
}

export function PipeConfsPage(props) {
  return (
    <SearchPage {...props}>
      <PipeConfs {...props} />
    </SearchPage>
  );
}

