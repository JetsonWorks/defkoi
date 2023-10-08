// ref: https://www.cluemediator.com/how-to-access-url-parameters-in-the-class-component-using-react-router-v6
import React from 'react';
import { useParams } from 'react-router';

const withRouter = WrappedComponent => props => {
  const params = useParams();
  return (
    <WrappedComponent {...props} params={params} />
  );
}
export default withRouter;
