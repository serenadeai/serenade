import React, { createContext, useContext, useReducer } from "react";

const initialState = {
  language: "Python",
};

const reducer = (state, action) => {
  switch (action.type) {
    case "language":
      return { ...state, language: action.language };
    default:
      throw new Error();
  }
};

export const StoreContext = createContext({});

export const StoreProvider = ({ children }) => {
  const [state, dispatch] = useReducer(reducer, initialState);
  return <StoreContext.Provider value={{ state, dispatch }}>{children}</StoreContext.Provider>;
};

export const useStore: any = () => useContext(StoreContext);
