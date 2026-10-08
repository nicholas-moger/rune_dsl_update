package chaos.s08.a4none;

import chaos.s08.a4none.meta.C8BoxMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Self-contained helper - relocated by the import axis.
 * @version 0.0.0
 */
@RosettaDataType(value="C8Box", builder=C8Box.C8BoxBuilderImpl.class, version="0.0.0")
@RuneDataType(value="C8Box", model="chaos", builder=C8Box.C8BoxBuilderImpl.class, version="0.0.0")
public interface C8Box extends RosettaModelObject {

	C8BoxMeta metaData = new C8BoxMeta();

	/*********************** Getter Methods  ***********************/
	String getLid();

	/*********************** Build Methods  ***********************/
	C8Box build();
	
	C8Box.C8BoxBuilder toBuilder();
	
	static C8Box.C8BoxBuilder builder() {
		return new C8Box.C8BoxBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C8Box> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C8Box> getType() {
		return C8Box.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("lid"), String.class, getLid(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C8BoxBuilder extends C8Box, RosettaModelObjectBuilder {
		C8Box.C8BoxBuilder setLid(String lid);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("lid"), String.class, getLid(), this);
		}
		

		C8Box.C8BoxBuilder prune();
	}

	/*********************** Immutable Implementation of C8Box  ***********************/
	class C8BoxImpl implements C8Box {
		private final String lid;
		
		protected C8BoxImpl(C8Box.C8BoxBuilder builder) {
			this.lid = builder.getLid();
		}
		
		@Override
		@RosettaAttribute("lid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("lid")
		public String getLid() {
			return lid;
		}
		
		@Override
		public C8Box build() {
			return this;
		}
		
		@Override
		public C8Box.C8BoxBuilder toBuilder() {
			C8Box.C8BoxBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C8Box.C8BoxBuilder builder) {
			ofNullable(getLid()).ifPresent(builder::setLid);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C8Box _that = getType().cast(o);
		
			if (!Objects.equals(lid, _that.getLid())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (lid != null ? lid.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C8Box {" +
				"lid=" + this.lid +
			'}';
		}
	}

	/*********************** Builder Implementation of C8Box  ***********************/
	class C8BoxBuilderImpl implements C8Box.C8BoxBuilder {
	
		protected String lid;
		
		@Override
		@RosettaAttribute("lid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("lid")
		public String getLid() {
			return lid;
		}
		
		@RosettaAttribute("lid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("lid")
		@Override
		public C8Box.C8BoxBuilder setLid(String _lid) {
			this.lid = _lid == null ? null : _lid;
			return this;
		}
		
		@Override
		public C8Box build() {
			return new C8Box.C8BoxImpl(this);
		}
		
		@Override
		public C8Box.C8BoxBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C8Box.C8BoxBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getLid()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C8Box.C8BoxBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C8Box.C8BoxBuilder o = (C8Box.C8BoxBuilder) other;
			
			
			merger.mergeBasic(getLid(), o.getLid(), this::setLid);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C8Box _that = getType().cast(o);
		
			if (!Objects.equals(lid, _that.getLid())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (lid != null ? lid.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C8BoxBuilder {" +
				"lid=" + this.lid +
			'}';
		}
	}
}
