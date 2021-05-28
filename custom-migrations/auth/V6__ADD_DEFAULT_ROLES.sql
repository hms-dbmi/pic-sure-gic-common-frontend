use auth;

SET @uuidRule = REPLACE(UUID(),'-','');
INSERT INTO access_rule VALUES (
    unhex(@uuidRule),
    'AR_INFO_COLUMN_LISTING',
    'allow query to info_column_listing',
    '$.query.query.expectedResultType',
    4,
    'INFO_COLUMN_LISTING',
    0,
    1,
    NULL,
    0,
    0
  );

SET @uuidPriv = REPLACE(UUID(),'-','');
INSERT INTO privilege (uuid, name, description, application_id)
	VALUES ( unhex(@uuidPriv),
		'PRIV_INFO_COLUMN_LISTING',
		'Allow access to variant info metadata',
		(SELECT uuid FROM application WHERE name = 'PICSURE')
	);

INSERT INTO accessRule_privilege (privilege_id, accessRule_id)
	VALUES (
		unhex(@uuidPriv),
		unhex(@uuidRule)
	);
	

 SET @uuidRole = REPLACE(UUID(),'-','');
  INSERT INTO role VALUES ( 
      unhex(@uuidRole), 
     'INFO_COLUMN_LISTING', 
     'Allow access to info column metadata' 
  );

INSERT INTO role_privilege (role_id, privilege_id)
	VALUES (
		unhex(@uuidRole),
		unhex(@uuidPriv)
	);



SET @uuidRule = REPLACE(UUID(),'-','');
INSERT INTO access_rule VALUES (
    unhex(@uuidRule),
    'AR_ONLY_SEARCH',
    'Can only do /search',
    ' $.[\'Target Service\']',
    6,
    '/search',
    0,
    0,
    NULL,
    0,
    0
  );

SET @uuidPriv = REPLACE(UUID(),'-','');
INSERT INTO privilege (uuid, name, description, application_id)
	VALUES ( unhex(@uuidPriv),
		'PRIV_ONLY_SEARCH',
		'Allow access to the /search/ function',
		(SELECT uuid FROM application WHERE name = 'PICSURE')
	);

INSERT INTO accessRule_privilege (privilege_id, accessRule_id)
	VALUES (
		unhex(@uuidPriv),
		unhex(@uuidRule)
	);
	

 SET @uuidRole = REPLACE(UUID(),'-','');
  INSERT INTO role VALUES ( 
      unhex(@uuidRole), 
     'SEARCH_ONLY', 
     'Allow access to the /search/ function' 
  );

INSERT INTO role_privilege (role_id, privilege_id)
	VALUES (
		unhex(@uuidRole),
		unhex(@uuidPriv)
	);
